package db

import doobie.Transactor
import doobie.hikari.HikariTransactor
import config.PGConfig.{DBConfig, HierarchyDBConfig}
import zio._
import zio.interop.catz._
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import java.sql.DriverManager

sealed trait DBTransactor {
  def xa: Transactor[Task]
}

trait HierarchyDBTransactor extends DBTransactor

object DBTransactor {

  val hierarchyXaLive: URLayer[HierarchyDBConfig, HierarchyDBTransactor] = ZLayer.scoped {
    for {
      config    <- ZIO.service[HierarchyDBConfig]
      be        <- ZIO.blockingExecutor
      hierarchy <- makeTransactor(config, be)
    } yield new HierarchyDBTransactor {
      override def xa: Transactor[Task] = hierarchy
    }
  }

  private def makeTransactor(config: DBConfig, be: Executor) = for {
    tr <- HikariTransactor
            .newHikariTransactor[Task](
              driverClassName = config.driverClassName,
              url = config.url.getOrElse(s"jdbc:postgresql://${config.host}:${config.port}/${config.db}"),
              user = config.user,
              pass = config.password,
              connectEC = be.asExecutionContext
            )
            .toScopedZIO
            .orDie
    _  <- ZIO.succeed(config match {
            case conf: HierarchyDBConfig => liquibaseUpdate(conf)
            case _                       => ()
          })
  } yield tr

  private def liquibaseUpdate(config: HierarchyDBConfig): Unit = {
    val changelogFile = "migrations/Changelog.xml"
    val classLoader   = getClass.getClassLoader
    val liquibase     = new Liquibase(
      changelogFile,
      new ClassLoaderResourceAccessor(classLoader),
      DatabaseFactory
        .getInstance()
        .findCorrectDatabaseImplementation(
          new JdbcConnection(
            DriverManager.getConnection(config.url.getOrElse(""), config.user, config.password)
          )
        )
    )
    liquibase.update("main")
    liquibase.close()
  }
}
