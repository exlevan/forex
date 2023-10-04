package forex.programs.rates

import cats.Monad
import cats.effect.kernel.{ Deferred, DeferredSink }
import cats.implicits.*
import forex.config.OneFrameConfig
import forex.domain.*
import forex.programs.rates.errors.*
import forex.services.rate_cache.CachedRate
import forex.services.{ RateCacheService, RatesService }

import java.time.OffsetDateTime

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    rateCacheService: RateCacheService[F],
    oneFrameConfig: OneFrameConfig
) extends Algebra[F] {

  def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {

    val pair = Rate.Pair(from = request.from, to = request.to)

    def retrieveCachedRate(): F[Either[Error, CachedRate]] = {

      def retrieveCachedRateFromDeferredSource(source: Deferred[F, Option[CachedRate]]): F[Either[Error, CachedRate]] =
        for {
          cachedRateOpt <- source.get
          cachedRateOptFiltered =
            cachedRateOpt.filter { cachedRate =>
              cachedRate.timestamp.value
                .plusMinutes(5)
                .isAfter(OffsetDateTime.now)
            }
          cachedRate <-
            cachedRateOptFiltered match {
              case None =>
                rateCacheService.removeCachedRate(pair, source) >>
                  retrieveCachedRate()
              case Some(cachedRate) => Right(cachedRate).pure[F]
            }
        } yield cachedRate

      def cacheLiveRates(sink: DeferredSink[F, Option[CachedRate]]): F[Either[Error, CachedRate]] = {

        def handleResponse(
            pairSinks: List[(Rate.Pair, DeferredSink[F, Option[CachedRate]])],
            response: Map[Rate.Pair, Rate]
        ): F[Either[Error, CachedRate]] =
          for {
            case () <- pairSinks
              .traverse_ { case (pair, sink) =>
                sink.complete {
                  response.get(pair).map { rate =>
                    CachedRate(pair, rate.price, rate.timestamp)
                  }
                }
              }
          } yield response.get(pair) match {
            case None =>
              Left(
                Error.RateLookupFailed(
                  s"One-frame response missed the requested currency pair: ('${pair.from}', '${pair.to}')"
                )
              )
            case Some(rate) => Right(CachedRate(pair, rate.price, rate.timestamp))
          }

        for {
          pairSinks <-
            rateCacheService
              .mostWantedUncachedRates(oneFrameConfig.ratesRequestPairLimit - 1, pair)
              .map((pair, sink) :: _)
          response <- ratesService.getRates(pairSinks.map(_._1))
          cachedRate <-
            response match {
              case Left(err) =>
                pairSinks.traverse_ { case (_, sink) =>
                  sink.complete(None)
                } >> Left(toProgramError(err)).pure[F]
              case Right(response) =>
                handleResponse(pairSinks, response)
            }
        } yield cachedRate
      }

      for {
        cachedRateDeferred <- rateCacheService.getOrCreateCachedRate(pair)
        cachedRate <-
          cachedRateDeferred.fold(
            cacheLiveRates,
            retrieveCachedRateFromDeferredSource
          )
      } yield cachedRate
    }

    for {
      _ <- rateCacheService.incrementPairRequestCount(pair)
      rateEither <- retrieveCachedRate()
    } yield rateEither.map { cachedRate =>
      Rate(pair, cachedRate.price, cachedRate.timestamp)
    }
  }

}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      rateCacheService: RateCacheService[F],
      oneFrameConfig: OneFrameConfig
  ): Algebra[F] = new Program[F](ratesService, rateCacheService, oneFrameConfig)

}
