package forex.services.rates

import forex.domain.Rate
import errors.*

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}
