import Dependencies._
import sbt.Keys.version

ThisBuild / scalaVersion := "2.13.10"

lazy val scalaFmtSettings = Seq(
  scalafmtOnCompile      := true,
  scalafmtLogOnEachError := true
)

lazy val relationalHierarchy = project
  .in(file("relational-hierarchy"))
  .enablePlugins(JavaAppPackaging)
  .settings(scalaFmtSettings)
  .settings(
    name    := "relational-hierarchy",
    version := "0.1.0-SNAPSHOT"
  )
  .settings(
    libraryDependencies += zio,
    libraryDependencies += zioStreams,
    libraryDependencies ++= zioConfig,
    libraryDependencies ++= logging,
    libraryDependencies ++= circe,
    libraryDependencies += circeExtras,
    libraryDependencies ++= monocle,
    libraryDependencies ++= doobie,
    libraryDependencies += cats3Interop,
    libraryDependencies += pgDriver,
    libraryDependencies += liquibase,
    libraryDependencies += chimney,
    libraryDependencies += scunk
  )
