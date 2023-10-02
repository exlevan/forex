package forex.http
package rates

import cats.MonadError
import cats.implicits.*
import forex.programs.RatesProgram
import forex.programs.rates.Protocol as RatesProgramProtocol
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: MonadError[*[*], Throwable]](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters.*, QueryParams.*, Protocol.*

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromVld) +& ToQueryParam(toVld) =>
      (fromVld, toVld).mapN { (from, to) =>
        val protocolRequest = RatesProgramProtocol.GetRatesRequest(from, to)
        for {
          rateEither <- rates.get(protocolRequest)
          rate <- rateEither.liftTo[F]
          response <- Ok(rate.asGetApiResponse)
        } yield response
      }.orBadRequest
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
