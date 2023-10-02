package forex.config

import com.comcast.ip4s.{ Host, Port }
import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(http: HttpConfig, oneFrame: OneFrameConfig)

case class HttpConfig(host: Host, port: Port, timeout: FiniteDuration)

case class OneFrameConfig(baseUri: Uri, ratesRequestPairLimit: Int, authToken: String)
