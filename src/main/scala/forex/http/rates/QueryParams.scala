package forex.http.rates

import cats.data.ValidatedNel
import cats.implicits.*
import forex.domain.Currency
import forex.http.{ MissingQueryParameterException, MissingQueryParameterValueException }
import org.http4s.{ ParseFailure, QueryParamDecoder, QueryParameterValue }

object QueryParams {

  private[http] implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { currency =>
      Currency
        .fromString(currency)
        .toRight(
          ParseFailure(
            "Invalid currency",
            s"Currency parse failed. $currency is not a member of ${Currency.supportedCurrencies}"
          )
        )
    }

  /** Query parameter extractor using [[org.http4s.QueryParamDecoder]].
    *
    * Similar to [[org.http4s.dsl.io.ValidatingQueryParamDecoderMatcher]], but an absent parameter is treated as a
    * validation error.
    */
  abstract class RequiredValidatingQueryParamDecoderMatcher[T: QueryParamDecoder](name: String) {
    def unapply(
        params: Map[String, collection.Seq[String]]
    ): Some[ValidatedNel[Exception, T]] = {
      val decodedValueEither =
        for {
          values <- params.get(name).toRightNel(new MissingQueryParameterException(name))
          value <- values.headOption.toRightNel(new MissingQueryParameterValueException(name))
          decodedValue <- QueryParamDecoder[T].decode(QueryParameterValue(value)).toEither
        } yield decodedValue
      Some(decodedValueEither.toValidated)
    }
  }

  object FromQueryParam extends RequiredValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends RequiredValidatingQueryParamDecoderMatcher[Currency]("to")
}
