package forex.http

import cats.arrow.FunctionK
import cats.effect.SyncIO
import cats.implicits.*
import forex.domain.{ Currency, Rate }
import forex.programs.rates.{ Algebra, Protocol }
import forex.programs.rates.errors.Error as RatesProgramError
import io.circe.Decoder
import io.circe.parser.decode
import org.http4s.{ HttpRoutes, Request, Response }
import weaver.FunSuite

/** Common utilities and helpers for testing HttpRoutes
  */
trait HttpRoutesSuite extends FunSuite {

  /** A pure effect type used for testing HttpRoutes
    */
  type F[T] = Either[Throwable, T]

  /** Supplies provided routes with a request, returning a response. Throws an exception in case of an error.
    */
  def runRoutes(routes: HttpRoutes[F], request: Request[F]): Response[F] =
    routes.orNotFound.run(request).valueOr(throw _)

  /** Consumes a response body and converts is to a single [[String]]
    */
  def bodyString(response: Response[F]): String = {

    val eitherToSyncIO = new FunctionK[F, SyncIO] {
      def apply[A](fa: Either[Throwable, A]): SyncIO[A] = SyncIO.fromEither(fa)
    }

    response.bodyText.translate(eitherToSyncIO).compile.string.unsafeRunSync()
  }

  /** Consumes and parses a response body JSON. Throws an exception in case of an error.
    */
  def parseBody[T](response: Response[F])(
      implicit
      decoder: Decoder[T]
  ): T =
    decode(bodyString(response)).valueOr(throw _)

  implicit class CurrencyObjTest(currencyObj: Currency.type) {

    def fromStringUnsafe(currencyStr: String): Currency =
      currencyObj.fromString(currencyStr).getOrElse(sys.error("Invalid currency"))
  }

  trait RatesProgramStub extends Algebra[F] {
    def get(request: Protocol.GetRatesRequest): F[Either[RatesProgramError, Rate]] = ???
  }
}
