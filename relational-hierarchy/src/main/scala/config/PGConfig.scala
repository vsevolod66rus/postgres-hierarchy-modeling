package config

import config.PGConfig.HierarchyDBConfig

final case class PGConfig(hierarchy: HierarchyDBConfig)

object PGConfig {
  sealed trait DBConfig {
    def host: String
    def port: Int
    def db: String
    def url: Option[String]
    def user: String
    def password: String
    def driverClassName: String = "org.postgresql.Driver"
  }

  final case class HierarchyDBConfig(
      host: String,
      port: Int,
      db: String,
      url: Option[String],
      user: String,
      password: String
  ) extends DBConfig
}
