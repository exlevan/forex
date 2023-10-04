package forex

import cats.data.OptionT
import cats.effect.{ Sync, Temporal }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs.*
import forex.services.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.{ AutoSlash, ErrorHandling, Timeout }

class Module[F[_]: Lambda[T[*] => Sync[T] & Temporal[T]]](
    config: ApplicationConfig,
    httpClient: Client[F]
) {

  private val ratesService: RatesService[F] = RatesServices.live[F](config.oneFrame, httpClient)

  private val rateCacheService: RateCacheService[F] = RateCacheService[F]()

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, rateCacheService, config.oneFrame)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { (http: HttpRoutes[F]) =>
    val http4sDsl: Http4sDsl[F] = Http4sDsl[F]
    import http4sDsl.*

    ErrorHandling.Custom.recoverWith(AutoSlash(http)) { case err: Throwable =>
      OptionT.liftF(Status.InternalServerError.apply(err.getMessage))
    }
  }

  private val appMiddleware: TotalMiddleware = { (http: HttpApp[F]) =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  val asyncEffects: F[Unit] = rateCacheService.decayRequestCounts
}
