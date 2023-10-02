package forex

import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Console
import cats.implicits.*
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs.*
import forex.services.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.implicits.*
import org.http4s.server.middleware.{ AutoSlash, ErrorAction, Timeout }

class Module[F[_]: Async: Console](
    config: ApplicationConfig,
    httpClient: Client[F]
) {

  private val ratesService: RatesService[F] = RatesServices.live[F](config.oneFrame, httpClient)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { (http: HttpRoutes[F]) =>
    val console = Console[OptionT[F, *]]

    ErrorAction.log(
      AutoSlash(http),
      messageFailureLogAction = (t, msg) =>
        console.println(msg) >>
          console.println(t),
      serviceErrorLogAction = (t, msg) =>
        console.println(msg) >>
          console.println(t)
    )
  }

  private val appMiddleware: TotalMiddleware = { (http: HttpApp[F]) =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
