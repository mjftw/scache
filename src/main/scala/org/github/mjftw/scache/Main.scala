package org.github.mjftw.scache

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}

import scala.concurrent.duration._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.put("foo", 1)
      _ <- IO.sleep(1.second)
      _ <- cache.put("bar", 3)
      _ <- IO.sleep(1500.millis)
      x <- cache.get("foo")
      y <- cache.get("bar")
      _ <- IO(println(x, y))
    } yield ExitCode.Success
}
