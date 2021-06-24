package org.github.mjftw.scache

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}

import scala.concurrent.duration._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.second)
      _ <- cache.put("bar", 1)
      _ <- IO.sleep(1.second)
      _ <- cache.putWithExpiry("baz", 3, 2.seconds)
      _ <- IO.sleep(1500.millis)
      foo <- cache.get("foo")
      bar <- cache.get("bar")
      baz <- cache.get("baz")
      _ <- IO(println(s"foo: $foo. bar: $bar, baz: $baz"))
    } yield ExitCode.Success
}
