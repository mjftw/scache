package org.github.mjftw.scache

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect._
import cats.effect.laws.util.TestContext
import scala.concurrent.duration._
import scala.util.Success

class CacheSpec extends AnyFlatSpec with Matchers {
  implicit val ctx = TestContext()
  implicit val cs: ContextShift[IO] = ctx.ioContextShift
  implicit val timer: Timer[IO] = ctx.timer

  "put and get" should "store and retrieve values" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.put("foo", 1)
      x <- cache.get("foo")
    } yield x).unsafeRunSync

    result should be(Some(1))
  }

  it should "return None when on cache miss" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.put("foo", 1)
      x <- cache.get("bar")
    } yield x).unsafeRunSync

    result should be(None)
  }

  it should "replace values when putting the same key twice" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.put("foo", 1)
      _ <- cache.put("foo", 2)
      x <- cache.get("foo")
    } yield x).unsafeRunSync

    result should be(Some(2))
  }

  it should "prevent key removal when overwriting a key scheduled for removal" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.second)
      _ <- cache.put("foo", 2)
      _ <- IO.sleep(2.seconds)
      x <- cache.get("foo")
    } yield x).unsafeToFuture()

    ctx.tick(3.seconds)

    result.value should be(Some(Success(Some(2))))
  }

  "putWithExpiry and get" should "store and retrieve values" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.minute)
      x <- cache.get("foo")
    } yield x).unsafeRunSync

    result should be(Some(1))
  }

  it should "return None when on cache miss" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.minute)
      x <- cache.get("bar")
    } yield x).unsafeRunSync

    result should be(None)
  }

  it should "replace values when putting the same key twice" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.minute)
      _ <- cache.putWithExpiry("foo", 2, 1.minute)
      x <- cache.get("foo")
    } yield x).unsafeRunSync

    result should be(Some(2))
  }

  it should "expire cache values" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.second)
      x <- cache.get("foo")
      _ <- IO.sleep(1001.millis)
      y <- cache.get("foo")
    } yield (x, y)).unsafeToFuture

    ctx.tick(2.second)

    result.value should be(Some(Success((Some(1), None))))
  }

  it should "replace the cache invalidation delay when overwriting values" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.putWithExpiry("foo", 1, 1.second)
      x <- cache.get("foo")
      _ <- cache.putWithExpiry("foo", 2, 2.second)
      _ <- IO.sleep(1001.millis)
      y <- cache.get("foo")
    } yield (x, y)).unsafeToFuture

    ctx.tick(3.second)

    result.value should be(Some(Success((Some(1), Some(2)))))
  }
  it should "add a cache invalidation delay when overwriting non timed values" in {
    val result = (for {
      cache <- Cache.make[IO, String, Int]
      _ <- cache.put("foo", 1)
      x <- cache.get("foo")
      _ <- cache.putWithExpiry("foo", 2, 1.second)
      _ <- IO.sleep(1001.millis)
      y <- cache.get("foo")
    } yield (x, y)).unsafeToFuture

    ctx.tick(3.second)

    result.value should be(Some(Success((Some(1), None))))
  }

}
