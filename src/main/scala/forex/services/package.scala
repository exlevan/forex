package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  val RatesServices = rates.Interpreters

  type RateCacheService[F[_]] = rate_cache.Algebra[F]
  val RateCacheService = rate_cache.Service
}
