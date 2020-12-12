import sbt._

object ProjectConfig {

  object versions {
    val akka              = "2.6.9"
    val `akka-http`       = "10.2.1"
    val `akka-management` = "1.0.9"
    val akkaJournal       = "1.8"

    val `akka-http-play-json` = "1.34.0"
    val `play-json`           = "2.9.0"

    val slick         = "3.3.2"
    val `flyway-core` = "6.5.5"
    val h2            = "1.4.200"

    val scalactic         = "3.2.0"
    val scalatest         = "3.2.0"
    val scalacheck        = "1.14.3"
    val `scalacheck-1-14` = "3.2.0.0"
    val scalamock         = "5.0.0"

    val `scalacheck-shapeless_1.14` = "1.2.3"

    val logback = "1.2.3"

    val slf4j = "1.7.30"

    val testcontainers = "0.38.1"

    val scalaxml = "2.0.0-M1"

    val tapir = "0.17.0-M2"
  }

  val testDependencies = Seq(
    "org.scalactic"              %% "scalactic"                 % versions.scalactic                   % Test,
    "org.scalatest"              %% "scalatest"                 % versions.scalatest                   % Test,
    "org.scalacheck"             %% "scalacheck"                % versions.scalacheck                  % Test,
    "org.scalatestplus"          %% "scalacheck-1-14"           % versions.`scalacheck-1-14`           % Test,
    "org.scalamock"              %% "scalamock"                 % versions.scalamock                   % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % versions.`scalacheck-shapeless_1.14` % Test
  )

  val akkaDependencies = Seq(
    "com.typesafe.akka"             %% "akka-stream"                  % versions.akka,
    "com.typesafe.akka"             %% "akka-http"                    % versions.`akka-http`,
    "com.typesafe.akka"             %% "akka-http-spray-json"         % versions.`akka-http`,
    "com.typesafe.akka"             %% "akka-http2-support"           % versions.`akka-http`,
    "com.typesafe.akka"             %% "akka-discovery"               % versions.akka,
    "com.typesafe.akka"             %% "akka-stream-testkit"          % versions.akka,
    "com.typesafe.akka"             %% "akka-http-testkit"            % versions.`akka-http`,
    "com.typesafe.akka"             %% "akka-slf4j"                   % versions.akka,
    "com.typesafe.akka"             %% "akka-actor-typed"             % versions.akka,
    "com.typesafe.akka"             %% "akka-cluster-typed"           % versions.akka,
    "com.typesafe.akka"             %% "akka-cluster-sharding-typed"  % versions.akka,
    "com.typesafe.akka"             %% "akka-persistence-typed"       % versions.akka,
    "com.typesafe.akka"             %% "akka-serialization-jackson"   % versions.akka,
    "com.typesafe.akka"             %% "akka-persistence-query"       % versions.akka,
    "com.lightbend.akka.management" %% "akka-management"              % versions.`akka-management`,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % versions.`akka-management`
  )

  val akkaJournal = Seq(
    "org.fusesource.leveldbjni" % "leveldbjni-all" % versions.akkaJournal
  )

  val playJsonDependencies = Seq(
    "de.heikoseeberger" %% "akka-http-play-json" % versions.`akka-http-play-json`,
    "com.typesafe.play" %% "play-json"           % versions.`play-json`
  )

  val logDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "org.slf4j"      % "slf4j-api"       % versions.slf4j
  )

  val slickDependencies = Seq(
    "com.typesafe.slick" %% "slick"          % versions.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % versions.slick
  )

  val dbDependencies = Seq(
    "org.flywaydb"   % "flyway-core" % versions.`flyway-core`,
    "com.h2database" % "h2"          % versions.h2
  )

  val xmlDependencies = Seq(
    "org.scala-lang.modules" %% "scala-xml" % versions.scalaxml
  )

  val docDependencies = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core"                 % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-play"            % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server"     % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"         % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"   % versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % versions.tapir
  )

  val projectDependencies: Seq[ModuleID] =
    testDependencies ++
      akkaDependencies ++
      akkaJournal ++
      playJsonDependencies ++
      slickDependencies ++
      dbDependencies ++
      logDependencies ++
      xmlDependencies ++
      docDependencies
}
