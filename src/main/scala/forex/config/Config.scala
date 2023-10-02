package forex.config

import cats.effect.Sync

import pureconfig.ConfigSource
import pureconfig.generic.auto.*
import pureconfig.module.catseffect.syntax.*
import pureconfig.module.ip4s.*

object Config {

  /** @param path
    *   the property path inside the default configuration
    */
  def load[F[_]: Sync](path: String): F[ApplicationConfig] =
    ConfigSource.default.at(path).loadF[F, ApplicationConfig]()
}
