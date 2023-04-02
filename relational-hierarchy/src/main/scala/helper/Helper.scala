package helper

import zio._
import java.util.concurrent.TimeUnit
object Helper {
  def duration[R, E, A](tag: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] = for {
    clock <- ZIO.clock
    start <- clock.currentTime(TimeUnit.MILLISECONDS)
    res   <- effect
    end   <- clock.currentTime(TimeUnit.MILLISECONDS)
    _     <- duration(start, end, tag)
  } yield res

  private def duration(start: Long, end: Long, tag: String): UIO[Unit] = for {
    milliAll <- ZIO.succeed(end - start)
    min       = milliAll / 60000
    sec       = milliAll / 1000 % 60
    milli     = milliAll        % 1000
    _        <- ZIO.logInfo(s"$tag prepared for $min min $sec sec $milli milli")
  } yield ()
}
