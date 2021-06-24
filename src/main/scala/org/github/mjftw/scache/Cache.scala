package org.github.mjftw.scache

import cats.effect.concurrent.Ref
import cats.effect.Sync
import cats.implicits._
import scala.concurrent.duration.FiniteDuration
import cats.effect.Fiber
import cats.effect.Async
import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.LiftIO
import cats.effect.Timer

trait Cache[F[_], A, B] {
  def put(key: A, value: B): F[Unit]
  def get(key: A): F[Option[B]]
}

object Cache {
  /** Create a simple cache */
  def make[F[_]: Sync, A, B]: F[Cache[F, A, B]] =
    Ref.of[F, Map[A, B]](Map.empty[A, B]).map { ref =>
      new Cache[F, A, B] {
        def put(key: A, value: B) = ref.update(_ + (key -> value))
        def get(key: A): F[Option[B]] = ref.get.map(_.get(key))
      }
    }

  /** Create a cache whose entries are invalidated after an amount of time */
  def makeExpiring[F[_]: Concurrent: Timer, A, B](keyExpiry: FiniteDuration): F[Cache[F, A, B]] =
    Ref.of[F, Map[A, (B, Fiber[F, Unit])]](Map.empty[A, (B, Fiber[F, Unit])]).map { ref =>
      new Cache[F, A, B] {
        def put(key: A, value: B) =
          for {
            // Cancel any ongoing removal timer
            _ <- ref.get.map(_.get(key) match {
              case Some((_, fiber)) => fiber.cancel
              case None => Sync[F].unit
            }).flatten
            // Start a new removal timer
            fiber <- Concurrent[F].start {
              Timer[F].sleep(keyExpiry) *> remove(key)
            }
            _ <- ref.update(_ + (key -> Tuple2(value, fiber)))
        } yield ()

        def get(key: A): F[Option[B]] = ref.get.map(_.get(key).map{case (value, _) => value})

        private def remove(key: A): F[Unit] = ref.update(_ - key)

        private def delay[A](duration: FiniteDuration)(block: => A): F[A] =
          for {
            _ <- Timer[F].sleep(duration)
            a <- Sync[F].delay(block)
          } yield a
      }
    }
}