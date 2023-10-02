package forex.config

import com.comcast.ip4s.IpLiteralSyntax

import scala.concurrent.duration.*
import weaver.SimpleIOSuite

object ConfigSuite extends SimpleIOSuite {

  test("Config.load should parse and load a configuration") {

    val expectedConf = ApplicationConfig(
      http = HttpConfig(
        host = ipv4"0.0.0.0",
        port = port"8080",
        timeout = 40.seconds
      )
    )

    for {
      conf <- Config.load("correct-configuration")
    } yield expect(conf == expectedConf)
  }

  test("Config.load should raise an error for an incorrect configuration") {

    for {
      confEither <- Config.load("incorrect-configuration").attempt
    } yield expect(confEither.isLeft)
  }

  test("Config.load should raise an error for a missing configuration") {

    for {
      confEither <- Config.load("missing-configuration").attempt
    } yield expect(confEither.isLeft)
  }
}
