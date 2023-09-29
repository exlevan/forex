package forex.config

import cats.effect.Sync

import pureconfig.ConfigSource
import pureconfig.generic.auto.*

object Config {

  /** @param path
    *   the property path inside the default configuration
    */
  def load[F[_]: Sync](path: String): F[ApplicationConfig] =
    Sync[F].delay {
      ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]
    }
}
