package forex

import cats.effect.Async
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services.*
import forex.programs.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]](
    config: ApplicationConfig
)(
    implicit
    ev: Async[F]
) {

  private val ratesService: RatesService[F] = RatesServices.dummy[F]

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { (http: HttpRoutes[F]) =>
    AutoSlash(http)
  }

  // TODO: error handling

  private val appMiddleware: TotalMiddleware = { (http: HttpApp[F]) =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
