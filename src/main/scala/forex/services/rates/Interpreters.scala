package forex.services.rates

import cats.Applicative
import cats.effect.Concurrent
import forex.config.OneFrameConfig
import forex.services.rates.interpreters.*
import org.http4s.client.Client

import scala.annotation.unused

object Interpreters {
  @unused
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Concurrent](config: OneFrameConfig, httpClient: Client[F]): Algebra[F] =
    new OneFrameLive[F](config, httpClient)
}
