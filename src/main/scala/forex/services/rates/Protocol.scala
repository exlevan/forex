package forex.services.rates

import forex.domain.*
import io.circe.*
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  final case class GetRatesResponse(rates: List[GetRatesResponseRate])

  final case class GetRatesResponseRate(from: Currency, to: Currency, price: Price, timeStamp: Timestamp)

  implicit val currencyDecoder: Decoder[Currency] =
    implicitly[Decoder[String]].emap(Currency.fromString(_).toRight("Invalid currency"))

  implicit val responseRateDecoder: Decoder[GetRatesResponseRate] =
    deriveConfiguredDecoder[GetRatesResponseRate]

  implicit val responseDecoder: Decoder[GetRatesResponse] =
    implicitly[Decoder[List[GetRatesResponseRate]]].map(GetRatesResponse)

}
