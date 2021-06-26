# Scache

A lightweight effect based Scala cache, compatible with the [Typelevel ecosystem](https://typelevel.org/cats/typelevelEcosystem.html)

## Example

As a simple example, lets create an IOApp and try out the cache.

```scala
import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import scala.concurrent.duration._

object Example extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for {
      // Make a new cache
      cache <- Cache.make[IO, String, Int]

      // Put the 1 value in the cache with key "foo"
      _ <- cache.put("foo", 1)

      // Retrieve the value from the cache
      foo <- cache.get("foo")

      // Put the value 2 in the cache for 1 second
      _ <- cache.putWithExpiry("bar", 2, 1.second)

      // Wait 2 seconds - the cached value should expire
      _ <- IO.sleep(2.seconds)

      // Attempt to get the cached value - should have expired
      bar <- cache.get("bar")

      // Put the value 3 in the cache for 1 second
      _ <- cache.putWithExpiry("baz", 3, 1.seconds)

      // Wait 0.5 seconds - Mot enough time to expire the cached value
      _ <- IO.sleep(500.millis)

      // Retrieve the value from the cache
      baz <- cache.get("baz")

      // See the results
      _ <- IO(println(s"foo: $foo bar: $bar, baz: $baz"))
    } yield ExitCode.Success
}
```

Running the example program gives:

```scala
foo: Some(1) bar: None, foo2: Some(3)
```

The value for `foo` was retrieved, `bar` had expired so no value was
returned, and `baz` was retrieved as it had not yet expired.

In the example above `IO` was used as the effect, with Strings for keys, and Ints for values.
You can use whatever effect, key, and value types you like though.
