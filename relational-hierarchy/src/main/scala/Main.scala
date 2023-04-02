import config.AppConfig
import db.DBTransactor
import repo.HierarchyRepo
import zio.logging.backend.SLF4J
import zio.{Runtime, ZIO, ZIOAppDefault}

object Main extends ZIOAppDefault {

  private val loggerLayer = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val app = for {
    _ <- AppConfig.logConfig
    _ <- ZIO.serviceWithZIO[HierarchyRepo](_.hello)
  } yield ()

  override def run: ZIO[Any, Any, Unit] = app.provide(
    AppConfig.live,
    AppConfig.allConfigs,
    DBTransactor.hierarchyXaLive,
    HierarchyRepo.live,
    loggerLayer
  )
}
