package forex

import cats.Applicative
import cats.data.ValidatedNel
import cats.effect.Concurrent
import io.circe.generic.extras.decoding.{ EnumerationDecoder, UnwrappedDecoder }
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.{ Decoder, Encoder }
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, EntityEncoder, Response }

package object http {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Concurrent]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]]: EntityEncoder[F, A]             = jsonEncoderOf[F, A]

  implicit class ValidatedResponse[F[_]](response: ValidatedNel[Throwable, F[Response[F]]]) {

    val http4sDsl: Http4sDsl[F] = Http4sDsl[F]
    import http4sDsl.*

    def orBadRequest(
        implicit
        ev: Applicative[F]
    ): F[Response[F]] = response.valueOr { errors =>
      BadRequest(errors.map(_.getMessage).toList.mkString("\n"))
    }
  }
}
