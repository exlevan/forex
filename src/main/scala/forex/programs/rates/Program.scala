package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import errors.*
import forex.domain.*
import forex.services.RatesService

class Program[F[_]: Functor](ratesService: RatesService[F]) extends Algebra[F] {

  def get(request: Protocol.GetRatesRequest): F[Error Either Rate] =
    EitherT(ratesService.get(Rate.Pair(request.from, request.to))).leftMap(toProgramError).value

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}
