package forex.services.rates

import cats.Applicative
import interpreters.*

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]]: Algebra[F] = ???
}
