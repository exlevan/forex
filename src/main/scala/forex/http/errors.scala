package forex.http

class MissingQueryParameterException(name: String) extends RuntimeException {
  override def getMessage: String = s"Missing required query parameter '$name'"
}

class MissingQueryParameterValueException(name: String) extends RuntimeException {
  override def getMessage: String = s"Query parameter '$name' is missing a required value"
}
