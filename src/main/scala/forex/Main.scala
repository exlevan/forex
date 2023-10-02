package forex

import cats.effect.*
import forex.config.*
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.*
import org.http4s.server.Server

object Main extends ResourceApp.Forever {

  def run(args: List[String]): Resource[IO, Unit] =
    new Application[IO].run()
}

class Application[F[_]: Async: Network] {

  def run(): Resource[F, Unit] =
    for {
      config <- Resource.eval(Config.load("app"))
      module = new Module[F](config)
      _ <- server(config.http, module.httpApp)
    } yield ()

  def server(config: HttpConfig, app: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(app)
      .build
}
