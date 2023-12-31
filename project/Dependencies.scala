import sbt.*

object Dependencies {

  object Versions {
    val cats       = "2.10.0"
    val catsEffect = "3.5.2"
    val fs2        = "3.9.2"
    val http4s     = "0.23.23"
    val circe      = "0.14.6"
    val pureConfig = "0.17.4"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
    val logback          = "1.4.11"
    val scalaCheck       = "1.17.0"
    val weaverTest       = "0.8.3"
    val catsScalaCheck   = "0.3.2"
    val testContainers   = "0.41.0"
  }

  object Libraries {
    def circe(artifact: String, version: String = Versions.circe): ModuleID = "io.circe"   %% artifact % version
    def http4s(artifact: String): ModuleID                                  = "org.http4s" %% artifact % Versions.http4s
    def pureconfig(artifact: String): ModuleID = "com.github.pureconfig" %% artifact % Versions.pureConfig

    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2        = "co.fs2"        %% "fs2-core"    % Versions.fs2

    lazy val http4sDsl            = http4s("http4s-dsl")
    lazy val http4sServer         = http4s("http4s-ember-server")
    lazy val http4sClient         = http4s("http4s-ember-client")
    lazy val http4sCirce          = http4s("http4s-circe")
    lazy val circeCore            = circe("circe-core")
    lazy val circeGeneric         = circe("circe-generic")
    lazy val circeGenericExt      = circe("circe-generic-extras", "0.14.3")
    lazy val circeParser          = circe("circe-parser")
    lazy val pureConfig           = pureconfig("pureconfig")
    lazy val pureConfigCatsEffect = pureconfig("pureconfig-cats-effect")
    lazy val pureConfigIp4s       = pureconfig("pureconfig-ip4s")
    lazy val pureConfigHttp4s     = pureconfig("pureconfig-http4s")

    // Compiler plugins
    lazy val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % Versions.betterMonadicFor
    lazy val kindProjector    = "org.typelevel" % "kind-projector"     % Versions.kindProjector cross CrossVersion.full

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val weaverTest     = "com.disneystreaming" %% "weaver-cats"               % Versions.weaverTest
    lazy val testContainers = "com.dimafeng"        %% "testcontainers-scala-core" % Versions.testContainers
    lazy val scalaCheck     = "org.scalacheck"      %% "scalacheck"                % Versions.scalaCheck
    lazy val catsScalaCheck = "io.chrisdavenport"   %% "cats-scalacheck"           % Versions.catsScalaCheck
  }

}
