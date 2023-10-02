package forex.http

import cats.implicits.*
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.rates.Protocol.GetApiResponse
import forex.http.rates.RatesHttpRoutes
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.*
import forex.programs.rates.errors.Error as RatesProgramError
import org.http4s.implicits.*
import org.http4s.{ Method, Request, Status }

import scala.util.Try

object RatesHttpRoutesSuite extends HttpRoutesSuite {

  val currency1 = Currency.fromStringUnsafe("USD")
  val currency2 = Currency.fromStringUnsafe("JPY")

  val rate: Rate = Rate(
    pair = Rate.Pair(currency1, currency2),
    price = Price(123),
    timestamp = Timestamp.now
  )

  test("Get rates request should return a response") {

    val ratesProgram: RatesProgram[F] = new RatesProgramStub {
      override def get(request: GetRatesRequest): F[Either[RatesProgramError, Rate]] =
        Right(rate).pure[F]
    }

    val routes = new RatesHttpRoutes[F](ratesProgram).routes

    val response = runRoutes(
      routes,
      Request(
        method = Method.GET,
        uri = uri"/rates?from=USD&to=JPY"
      )
    )

    val getApiResponse = parseBody[GetApiResponse](response)

    expect.all(
      response.status == Status.Ok,
      getApiResponse.from == rate.pair.from,
      getApiResponse.to == rate.pair.to,
      getApiResponse.price == rate.price,
      getApiResponse.timestamp == rate.timestamp
    )
  }

  test("Get rates request should require 'from' and 'to' query parameters") {

    val ratesProgram: RatesProgram[F] = new RatesProgramStub {
      override def get(request: GetRatesRequest): F[Either[RatesProgramError, Rate]] =
        Right(rate).pure[F]
    }

    val routes = new RatesHttpRoutes[F](ratesProgram).routes

    val expectNoTo = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?from=USD"
        )
      )

      expect.all(
        response.status == Status.BadRequest,
        bodyString(response) == "Missing required query parameter 'to'"
      )
    }

    val expectNoFrom = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?to=USD"
        )
      )

      expect.all(
        response.status == Status.BadRequest,
        bodyString(response) == "Missing required query parameter 'from'"
      )
    }

    val expectNoBoth = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates"
        )
      )

      val errors = bodyString(response).split("\n")

      expect.all(
        response.status == Status.BadRequest,
        errors.size == 2,
        errors.toSet == Set(
          "Missing required query parameter 'from'",
          "Missing required query parameter 'to'"
        )
      )
    }

    List(
      expectNoTo,
      expectNoFrom,
      expectNoBoth
    ).combineAll
  }

  test("'from' and 'to' should be non-empty valid currency values") {

    val ratesProgram: RatesProgram[F] = new RatesProgramStub {
      override def get(request: GetRatesRequest): F[Either[RatesProgramError, Rate]] =
        Right(rate).pure[F]
    }

    val routes = new RatesHttpRoutes[F](ratesProgram).routes

    val expectNonEmptyValues = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?from&to="
        )
      )

      val errors = bodyString(response).split("\n")

      expect.all(
        response.status == Status.BadRequest,
        errors.size == 2
      )
    }

    val expectValidValues = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?from=DOLLAR&to=AAA"
        )
      )

      val errors = bodyString(response).split("\n")

      expect.all(
        response.status == Status.BadRequest,
        errors.size == 2
      )
    }

    val expectCaseInsensitive = {
      val response = runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?from=UsD&to=jpy"
        )
      )

      expect.all(
        response.status == Status.Ok
      )
    }

    List(
      expectNonEmptyValues,
      expectValidValues,
      expectCaseInsensitive
    ).combineAll
  }

  test("Program errors should result in a failure") {

    val ratesProgram: RatesProgram[F] = new RatesProgramStub {
      override def get(request: GetRatesRequest): F[Either[RatesProgramError, Rate]] =
        Left(RatesProgramError.RateLookupFailed("oops")).pure[F]
    }

    val routes = new RatesHttpRoutes[F](ratesProgram).routes

    val response = Try(
      runRoutes(
        routes,
        Request(
          method = Method.GET,
          uri = uri"/rates?from=USD&to=JPY"
        )
      )
    )

    expect(response.isFailure)
  }

}
