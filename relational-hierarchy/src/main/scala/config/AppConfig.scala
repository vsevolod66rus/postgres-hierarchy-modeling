package config

import monocle.macros.GenLens
import config.PGConfig.HierarchyDBConfig
import zio._
import zio.config._
import zio.config.magnolia.{Descriptor, descriptor}
import zio.config.typesafe._

final case class AppConfig(
    postgres: PGConfig
)

object AppConfig {

  private type AllConfigs = HierarchyDBConfig

  implicit private val configDescriptor = Descriptor[AppConfig]
    .mapClassName(toKebabCase)
    .mapFieldName(toKebabCase)

  val live: ULayer[AppConfig] = TypesafeConfig.fromResourcePath(descriptor[AppConfig]).orDie

  val allConfigs: URLayer[AppConfig, AllConfigs] = subConf(_.postgres.hierarchy)

  private def hidePasswords(config: AppConfig): AppConfig = {
    val setters = Seq(GenLens[AppConfig](_.postgres.hierarchy.password))
    setters.foldLeft(config)((conf, setter) => setter.replace("*****")(conf))
  }

  val logConfig: URIO[AppConfig, Unit] = for {
    appConfig <- ZIO.service[AppConfig].map(hidePasswords)
    config    <- ZIO
                   .fromEither(write(descriptor[AppConfig], appConfig))
                   .orDieWith(msg => new RuntimeException(s"Can't write config: $msg"))
    _         <- ZIO.logInfo(s"Application config:\n${config.toHoconString}")
  } yield ()

  private def subConf[T: Tag](accessor: AppConfig => T): URLayer[AppConfig, T] = ZLayer.fromFunction(accessor)

}
