package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative.*
import cats.syntax.either.*
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors.*

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(100), Timestamp.now).asRight[Error].pure[F]

}
