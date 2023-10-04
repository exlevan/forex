package forex.services.rates

import forex.domain.Rate.Pair

object errors {

  sealed trait Error extends Exception with Product

  object Error {
    final case class OneFrameRatesPairLimitExceeded(num: Int, maxNum: Int) extends Error
    final case class OneFrameInvalidPair(pair: Pair, msg: String) extends Error
    final case class OneFrameUnexpectedResponse(msg: String) extends Error
  }

}
