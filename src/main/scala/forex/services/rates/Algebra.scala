package forex.services.rates

import forex.domain.Rate
import errors.*

trait Algebra[F[_]] {
  def getRates(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]]
}
