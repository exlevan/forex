package forex.programs.rates

import cats.implicits.*
import forex.services.rates.errors.Error as RatesServiceError

object errors {

  sealed trait Error extends Exception with Product

  object Error {
    final case class RateLookupFailed(msg: String) extends Error {
      override def getMessage: String = msg
    }
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameRatesPairLimitExceeded(num, maxNum) =>
      Error.RateLookupFailed(show"Can't fit $num currency pairs into request: only $maxNum is allowed")
    case RatesServiceError.OneFrameInvalidPair(pair, msg) =>
      Error.RateLookupFailed(show"Invalid currency pair ('${pair.from}', '${pair.to}'): $msg")
  }
}
