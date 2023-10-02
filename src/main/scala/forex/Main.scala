package forex

import cats.effect.*
import cats.effect.std.Console
import forex.config.*
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.*
import org.http4s.server.Server

object Main extends ResourceApp.Forever {

  def run(args: List[String]): Resource[IO, Unit] =
    new Application[IO].run()
}

class Application[F[_]: Async: Network: Console] {

  def run(): Resource[F, Unit] =
    for {
      config <- Resource.eval(Config.load("app"))
      httpClient <- mkHttpClient()
      module = new Module[F](config, httpClient)
      _ <- mkServer(config.http, module.httpApp)
    } yield ()

  def mkServer(config: HttpConfig, app: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(app)
      .build

  def mkHttpClient(): Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .build
}
