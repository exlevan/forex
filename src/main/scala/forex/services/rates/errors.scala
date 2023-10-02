package forex.services.rates

object errors {

  sealed trait Error extends Product with Serializable
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}
