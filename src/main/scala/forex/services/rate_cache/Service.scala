package forex.services.rate_cache
import cats.effect.{ Concurrent, Sync, Temporal }
import cats.implicits.*
import forex.domain.{ Currency, Rate }

import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*

class Service[F[_]: Lambda[T[*] => Sync[T] & Temporal[T]]] extends Algebra[F] {

  private val rateCache: ConcurrentHashMap[Rate.Pair, CacheEntry] = new ConcurrentHashMap()

  def getOrCreateCachedRate(
      pair: Rate.Pair
  ): F[EitherSinkSource] =
    for {
      newDeferred <- Concurrent[F].deferred[Option[CachedRate]]
      oldDeferredOpt <- Sync[F].delay(Option(rateCache.putIfAbsent(pair, newDeferred)))
    } yield oldDeferredOpt match {
      case None                   => Left(newDeferred)
      case Some(existingDeferred) => Right(existingDeferred)
    }

  def removeCachedRate(
      pair: Rate.Pair,
      oldValue: CacheEntry
  ): F[Unit] =
    Sync[F].delay(rateCache.remove(pair, oldValue): Unit)

  private val pairRequestCounts: ConcurrentHashMap[Rate.Pair, Double] = new ConcurrentHashMap()

  def incrementPairRequestCount(pair: Rate.Pair): F[Unit] =
    Sync[F].delay {
      pairRequestCounts.compute(
        pair,
        { (_: Rate.Pair, count: Double) =>
          Option(count).fold(1.0)(_ + 1.0)
        }.asJavaBiFunction
      ): Unit
    }

  def mostWantedUncachedRates(limit: Int, excludePair: Rate.Pair): F[List[(Rate.Pair, CacheEntry)]] = {

    def isCached(entry: java.util.Map.Entry[Rate.Pair, Double]): F[Boolean] = {
      val pair = entry.getKey

      Option(rateCache.get(pair)) match {
        case None => false.pure[F] // Value not in cache
        case Some(deferred) =>
          deferred.tryGet.flatMap {
            case None => true.pure[F] // Value is being computed in another thread
            case Some(cachedRateOpt) =>
              cachedRateOpt.filter { cachedRate =>
                cachedRate.timestamp.value
                  .plusMinutes(5)
                  .isAfter(OffsetDateTime.now)
              } match {
                case None =>
                  // Value computation resulted in error or value is out of date
                  Sync[F].blocking(rateCache.remove(pair, deferred)) >> false.pure[F]
                case Some(_) =>
                  // Value is in cache
                  true.pure[F]
              }
          }
      }

    }

    for {
      uncachedEntries <-
        List
          .from(pairRequestCounts.entrySet().asScala)
          .filterA { entry =>
            isCached(entry).map { cached =>
              !cached && entry.getKey != excludePair
            }
          }
      topUncachedPairs =
        uncachedEntries
          .sortWith(_.getValue > _.getValue)
          .take(limit)
          .map(_.getKey)
      additionalUncachedPairs = {
        val topUncachedPairsSet = topUncachedPairs.toSet
        List.fill(limit - topUncachedPairsSet.size) {
          LazyList
            .continually(
              Rate.Pair(
                Currency.randomCurrency(),
                Currency.randomCurrency()
              )
            )
            .find { pair =>
              pair.from != pair.to && !topUncachedPairsSet.contains(pair)
            }
            .get
        }
      }
      allUncachedPairs <-
        (topUncachedPairs ++ additionalUncachedPairs).traverse { pair =>
          for {
            deferred <- Concurrent[F].deferred[Option[CachedRate]]
            _ <- Sync[F].delay(rateCache.putIfAbsent(pair, deferred))
          } yield (pair, deferred)
        }
    } yield allUncachedPairs

  }

  /** Factor by which request counts are reduced hourly.
    *
    * Makes count values lose half of their value in a span of one week.
    */
  private val decayFactor: Double = Math.pow(0.5, 1.0 / (7 * 24))

  /** Value at which request count is considered too small to keep.
    *
    * Corresponds to less than one request per 2 weeks.
    */
  private val removalThreshold: Double = 0.5

  def decayRequestCounts: F[Unit] =
    for {
      case () <- Temporal[F].sleep(1.hour)
      case () <- Sync[F].blocking {
        pairRequestCounts.replaceAll({ (_: Rate.Pair, count: Double) =>
          count * decayFactor
        }.asJavaBiFunction)
      }
      case () <- Sync[F].blocking {
        pairRequestCounts
          .values()
          .removeIf({ (count: Double) =>
            count < removalThreshold
          }.asJavaPredicate): Unit
      }
      result <- decayRequestCounts
    } yield result
}

object Service {
  def apply[F[_]: Lambda[T[*] => Sync[T] & Temporal[T]]]() = new Service[F]
}
