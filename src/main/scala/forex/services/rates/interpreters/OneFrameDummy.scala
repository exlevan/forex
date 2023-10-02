package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative.*
import cats.syntax.either.*
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors.*

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  def getRates(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]] =
    pairs
      .map { pair =>
        pair -> Rate(pair, Price(100), Timestamp.now)
      }
      .toMap
      .asRight[Error]
      .pure[F]

}
