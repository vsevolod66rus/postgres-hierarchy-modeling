import Dependencies.Versions._
import sbt._

object Dependencies {

  object Versions {
    lazy val zioVersion          = "2.0.2"
    lazy val zioLoggingVersion   = "2.1.2"
    lazy val zioConfigVersion    = "3.0.1"
    lazy val logbackVersion      = "1.4.3"
    lazy val log4jVersion        = "2.19.0"
    lazy val circeVersion        = "0.14.1"
    lazy val monocleVersion      = "3.1.0"
    lazy val doobieVersion       = "1.0.0-M5"
    lazy val cats3InteropVersion = "3.3.0"
    lazy val pgDriverVersion     = "42.5.0"
    lazy val liquibaseVersion    = "4.12.0"
    lazy val chimneyVersion      = "0.6.2"
    lazy val scunkVersion        = "0.5.1"
  }

  lazy val zio: ModuleID        = "dev.zio" %% "zio"         % zioVersion
  lazy val zioStreams: ModuleID = "dev.zio" %% "zio-streams" % zioVersion

  lazy val logging: Seq[ModuleID] = Seq(
    "dev.zio"                 %% "zio-logging-slf4j" % zioLoggingVersion,
    "ch.qos.logback"           % "logback-classic"   % logbackVersion,
    "org.apache.logging.log4j" % "log4j-core"        % log4jVersion
  )

  lazy val zioConfig: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-config",
    "dev.zio" %% "zio-config-magnolia",
    "dev.zio" %% "zio-config-typesafe"
  ).map(_ % zioConfigVersion)

  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  lazy val circeExtras = "io.circe" %% "circe-generic-extras" % circeVersion

  lazy val monocle: Seq[ModuleID] = Seq(
    "dev.optics" %% "monocle-core"  % monocleVersion,
    "dev.optics" %% "monocle-macro" % monocleVersion
  )

  lazy val doobie: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-quill"
  ).map(_ % doobieVersion)

  lazy val cats3Interop = "dev.zio" %% "zio-interop-cats" % cats3InteropVersion

  lazy val pgDriver = "org.postgresql" % "postgresql" % pgDriverVersion

  lazy val liquibase = "org.liquibase" % "liquibase-core" % liquibaseVersion

  lazy val chimney = "io.scalaland" %% "chimney" % chimneyVersion

  lazy val scunk = "org.tpolecat" %% "skunk-core" % "0.5.1"

}
