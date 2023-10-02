package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits.*
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.Protocol.*
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.implicits.*
import org.http4s.{ Headers, Method, Request }

class OneFrameLive[F[_]: Concurrent](config: OneFrameConfig, httpClient: Client[F]) extends Algebra[F] {

  def getRates(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]] = {

    val eitherTResult: EitherT[F, Error, Map[Rate.Pair, Rate]] =
      for {

        case () <- {
          val pairsLength = pairs.length

          EitherT.cond[F](
            pairsLength <= config.ratesRequestPairLimit,
            (),
            OneFrameRatesPairLimitExceeded(pairsLength, config.ratesRequestPairLimit)
          )
        }

        case () <- EitherT
          .fromOption[F](pairs.find(pair => pair.from == pair.to), ())
          .map(pair => OneFrameInvalidPair(pair, "Duplicate currency"))
          .swap
          .leftWiden[Error]

        uri =
          config.baseUri
            .withPath(path"/rates")
            .withQueryParam(
              "pair",
              pairs.map { case Rate.Pair(from, to) =>
                show"$from$to"
              }
            )

        request =
          Request[F](
            method = Method.GET,
            uri = uri,
            headers = Headers("token" -> config.authToken)
          )

        response <- EitherT.liftF(httpClient.expect[GetRatesResponse](request))

      } yield response.rates.map { rate =>
        val pair = Rate.Pair(rate.from, rate.to)
        pair -> Rate(pair, rate.price, rate.timeStamp)
      }.toMap

    eitherTResult.value
  }
}
