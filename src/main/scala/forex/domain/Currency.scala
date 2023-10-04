package forex.domain

import cats.Show

import scala.util.Random

/** A monetary unit involved in exchange transactions.
  *
  * To create new instances, use [[Currency.fromString]].
  *
  * @param value
  *   a 3-letter uppercase currency code, as defined in ISO 4217
  *
  * @see
  *   [[https://en.wikipedia.org/wiki/ISO_4217 ISO 4217]]
  */
case class Currency private (value: String) extends AnyVal

object Currency {

  /** A set of currencies supported by the one-frame service.
    *
    * @see
    *   https://hub.docker.com/r/paidyinc/one-frame
    */
  val supportedCurrencies: Set[String] = Set(
    // format: off
    "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN",
    "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL",
    "BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY",
    "COP", "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD",
    "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GGP", "GHS",
    "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF",
    "IDR", "ILS", "IMP", "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "JOD",
    "JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT",
    "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD",
    "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN",
    "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK",
    "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR",
    "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLL", "SOS", "SPL", "SRD",
    "STN", "SVC", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY",
    "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VEF",
    "VND", "VUV", "WST", "XAF", "XCD", "XDR", "XOF", "XPF", "YER", "ZAR",
    "ZMW", "ZWD"
    // format: on
  )

  private val supportedCurrenciesArray: Array[String] = supportedCurrencies.toArray

  def randomCurrency(): Currency = {
    val i = Random.nextInt(supportedCurrenciesArray.length)
    Currency(supportedCurrenciesArray(i))
  }

  implicit val show: Show[Currency] = Show.show(_.value)

  def fromString(s: String): Option[Currency] = {
    val upperCase = s.toUpperCase
    if (supportedCurrencies.contains(upperCase)) {
      Some(Currency(upperCase))
    } else None
  }

}
