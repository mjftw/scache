package org.github.mjftw.scache

import cats.implicits._
import scala.concurrent.duration.FiniteDuration
import cats.effect.concurrent.Ref
import cats.effect.{Sync, Fiber, Concurrent, Timer}

trait Cache[F[_], K, V] {
  def put(key: K, value: V): F[Unit]
  def putWithExpiry(key: K, value: V, expireAfter: FiniteDuration): F[Unit]
  def get(key: K): F[Option[V]]
  def cancelExpiry(key: K): F[Boolean]
}

object Cache {

  type CacheFiber[F[_]] = Fiber[F, Unit]
  type CacheValue[F[_], V] = (V, CacheFiber[F])
  type CacheMap[F[_], K, V] = Map[K, CacheValue[F, V]]

  /** Create a cache whose entries are invalidated after an amount of time */
  def make[F[_]: Concurrent: Timer, K, V]: F[Cache[F, K, V]] =
    Ref
      .of[F, CacheMap[F, K, V]](Map.empty[K, CacheValue[F, V]])
      .map { ref =>
        new Cache[F, K, V] {

          /** Put a value in the cache */
          def put(key: K, value: V): F[Unit] =
            putValueAndFiber(key, value, Fiber(Sync[F].unit, Sync[F].unit))

          /** Put a value in the cache and expire it after some time */
          def putWithExpiry(key: K, value: V, expireAfter: FiniteDuration): F[Unit] =
            for {
              fiber <- scheduleRemoval(key, expireAfter)
              _ <- putValueAndFiber(key, value, fiber)
            } yield ()

          /** Retrieve a value from the cache */
          def get(key: K): F[Option[V]] = ref.get.map(_.get(key).map { case (value, _) => value })

          /** Prevent a cache entry at a given key from expiring.
            * Returns true if the key was found in the cache, false otherwise.
            */
          def cancelExpiry(key: K): F[Boolean] =
            for {
              oldCache <- ref.getAndUpdate(cache =>
                cache.get(key) match {
                  case Some((value, _)) =>
                    cache + (key -> Tuple2(value, Fiber(Sync[F].unit, Sync[F].unit)))
                  case None => cache
                }
              )
              wasFound <- oldCache.get(key) match {
                case Some((_, oldFiber)) => oldFiber.cancel.as(true)
                case None                => Sync[F].unit.as(false)
              }
            } yield wasFound

          /** Put replace the value and fiber at a key and cancel the old fiber */
          private def putValueAndFiber(key: K, value: V, fiber: CacheFiber[F]) =
            for {
              oldCache <- ref.getAndUpdate(cache => cache + (key -> Tuple2(value, fiber)))
              _ <- oldCache.get(key) match {
                case Some((_, oldFiber)) => oldFiber.cancel
                case None                => Sync[F].unit
              }
            } yield ()

          /** Schedule removal removal of a key */
          private def scheduleRemoval(key: K, removeAfter: FiniteDuration): F[Fiber[F, Unit]] =
            Concurrent[F].start {
              Timer[F].sleep(removeAfter) *> ref.update(_ - key)
            }
        }
      }
}
