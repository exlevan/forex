package forex

import cats.effect._
import com.comcast.ip4s._
import forex.config._
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server._
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
      .withHost(Ipv4Address.fromString(config.host).get)
      .withPort(Port.fromInt(config.port).get)
      .withHttpApp(app)
      .build
}
