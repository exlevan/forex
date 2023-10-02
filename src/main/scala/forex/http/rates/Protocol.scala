package forex.http
package rates

import cats.implicits.*
import forex.domain.*
import forex.domain.Rate.Pair
import io.circe.*
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiResponse(from: Currency, to: Currency, price: Price, timestamp: Timestamp)

  implicit val currencyCodec: Codec[Currency] =
    Codec.from(
      implicitly[Decoder[String]].emap(Currency.fromString(_).toRight("Invalid currency")),
      implicitly[Encoder[String]].contramap[Currency](_.show)
    )

  implicit val pairCodec: Codec[Pair] =
    deriveConfiguredCodec[Pair]

  implicit val rateCodec: Codec[Rate] =
    deriveConfiguredCodec[Rate]

  implicit val responseCodec: Codec[GetApiResponse] =
    deriveConfiguredCodec[GetApiResponse]
}
