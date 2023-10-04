package forex.services.rate_cache

import cats.effect.Deferred
import cats.effect.kernel.DeferredSink
import forex.domain.Rate

trait Algebra[F[_]] {

  protected type CacheEntry = Deferred[F, Option[CachedRate]]

  protected type EitherSinkSource = Either[DeferredSink[F, Option[CachedRate]], Deferred[F, Option[CachedRate]]]

  /** Returns or creates a cache entry for the requested currency pair.
    *
    * @return
    *   A [[Deferred]] [[CachedRate]], wrapped in [[Either]]. Left [[CachedRate]] is empty and must be completed by the
    *   caller. Right [[CachedRate]] is ready to be consumed.
    */
  def getOrCreateCachedRate(pair: Rate.Pair): F[EitherSinkSource]

  /** Removes a cache entry if it's equal to `oldValue`.
    */
  def removeCachedRate(
      pair: Rate.Pair,
      oldValue: CacheEntry
  ): F[Unit]

  /** Increments a number of requests for a currency pair.
    */
  def incrementPairRequestCount(pair: Rate.Pair): F[Unit]

  /** Returns a list of the most requested currency pairs not present in cache.
    *
    * For each currency pair, en empty [[Deferred]] is provided to be completed by the caller.
    *
    * @param limit
    *   Maximum number of entries to return
    */
  def mostWantedUncachedRates(limit: Int, excludePair: Rate.Pair): F[List[(Rate.Pair, CacheEntry)]]

  /** Reduce value of existing request counts to account for possible changes in usage
    *
    * Type `F[Nothing]` would be more appropriate here, as no value is ever returned. However, it breaks implicit
    * resolution of Functor/Monad instances.
    */
  def decayRequestCounts: F[Unit]
}
