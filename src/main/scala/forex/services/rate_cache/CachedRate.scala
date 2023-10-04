package forex.services.rate_cache

import forex.domain.{ Price, Rate, Timestamp }

case class CachedRate(pair: Rate.Pair, price: Price, timestamp: Timestamp)
