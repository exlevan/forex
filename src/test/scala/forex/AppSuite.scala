package forex

import cats.effect.{ IO, Resource }
import cats.implicits.*
import com.comcast.ip4s.Port
import com.dimafeng.testcontainers.*
import forex.config.{ ApplicationConfig, Config }
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*
import org.http4s.{ Request, Status, Uri }
import org.testcontainers.containers.wait.strategy.Wait
import weaver.IOSuite

import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration.*
import scala.util.{ Random, Try }

object AppSuite extends IOSuite {

  type Res = (GenericContainer, Client[IO])

  def sharedResource: Resource[IO, Res] = {

    val containerDef = GenericContainer.Def(
      "paidyinc/one-frame:latest",
      exposedPorts = List(8080),
      waitStrategy = Wait.forHttp("/").forStatusCode(404)
    )

    val containerRes = Resource.make {
      IO.blocking(containerDef.start())
    } { container =>
      IO.blocking(container.stop())
    }

    val httpClientRes = EmberClientBuilder
      .default[IO]
      .withIdleTimeInPool(0.seconds)
      .build

    (containerRes, httpClientRes).tupled
  }

  /** A set of reserved ports currently being checked for availability or used by a running application instance.
    *
    * Used to avoid race conditions during port checking.
    */
  val reservedPorts: ConcurrentHashMap[Int, Unit] = new ConcurrentHashMap()

  def availablePort(initPort: Port): Resource[IO, Port] = {

    def isPortAvailable(initPort: Int): IO[(Int, Boolean)] =
      IO.blocking {
        val port = initPort + Random.nextInt(300)
        // Try to reserve a port
        val isBeingChecked = Option(reservedPorts.putIfAbsent(port, ())).isDefined
        val isAvailable = !isBeingChecked && Try {
          new ServerSocket(port).close()
        }.isSuccess
        // If a port was reserved but it's busy, release it
        if (!isBeingChecked && !isAvailable) {
          reservedPorts.remove(port)
        }
        (port, isAvailable)
      }

    Resource.make {
      // keep probing until an available port is found
      isPortAvailable(initPort.value)
        .iterateUntil(_._2)
        .map { case (portValue, _) =>
          Port.fromInt(portValue).get
        }
    } { port =>
      IO.delay {
        reservedPorts.remove(port)
      }
    }
  }

  def containerConfig(availablePort: Port, container: GenericContainer)(
      config: ApplicationConfig
  ): ApplicationConfig = {

    val oneFrameConfig = config.oneFrame

    config.copy(
      oneFrame = oneFrameConfig.copy(
        baseUri = Uri.unsafeFromString(s"http://${container.containerIpAddress}:${container.mappedPort(8080)}/")
      ),
      http = config.http.copy(port = availablePort)
    )

  }

  test("Application should return 404 response for unknown routes").usingRes { case (_, client) =>
    new Application[IO].run().use { case () =>
      for {
        status <-
          client.status(
            Request[IO](uri = uri"http://localhost:8080/unknown-route")
          )
      } yield expect(status == Status.NotFound)
    }
  }

  test("Application should return rates for a valid rates request").usingRes { case (container, client) =>
    val app =
      for {
        config <- Resource.eval(Config.load[IO]("app"))
        port <- availablePort(config.http.port)
        _ <- new Application[IO].run(containerConfig(port, container))
      } yield port

    app.use { port =>
      for {
        status <-
          client.status(
            Request[IO](uri = Uri.unsafeFromString(s"http://localhost:$port/rates?from=USD&to=JPY"))
          )
      } yield expect(status == Status.Ok)
    }
  }

  test("Application should ignore the trailing slash in requests").usingRes { case (container, client) =>
    val app =
      for {
        config <- Resource.eval(Config.load[IO]("app"))
        port <- availablePort(config.http.port)
        _ <- new Application[IO].run(containerConfig(port, container))
      } yield port

    app.use { port =>
      for {
        status <-
          client.status(
            Request[IO](uri = Uri.unsafeFromString(s"http://localhost:$port/rates/?from=USD&to=JPY"))
          )
      } yield expect(status == Status.Ok)
    }
  }

  test("Application should not accept the duplicate currency").usingRes { case (container, client) =>
    val app =
      for {
        config <- Resource.eval(Config.load[IO]("app"))
        port <- availablePort(config.http.port)
        _ <- new Application[IO].run(containerConfig(port, container))
      } yield port

    app.use { port =>
      for {
        status <-
          client.status(
            Request[IO](uri = Uri.unsafeFromString(s"http://localhost:$port/rates/?from=USD&to=USD"))
          )
      } yield expect(status != Status.Ok)
    }
  }

}
