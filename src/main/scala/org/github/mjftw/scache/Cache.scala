package org.github.mjftw.scache

import cats.implicits._
import scala.concurrent.duration.FiniteDuration
import cats.effect.concurrent.Ref
import cats.effect.{Sync, Fiber, Concurrent, Timer}

trait Cache[F[_], A, B] {
  def put(key: A, value: B): F[Unit]
  def putWithExpiry(key: A, value: B, expireAfter: FiniteDuration): F[Unit]
  def get(key: A): F[Option[B]]
}

object Cache {

  /** Create a cache whose entries are invalidated after an amount of time */
  def make[F[_]: Concurrent: Timer, A, B]: F[Cache[F, A, B]] =
    Ref.of[F, Map[A, (B, Option[Fiber[F, Unit]])]](Map.empty[A, (B, Option[Fiber[F, Unit]])]).map {
      ref =>
        new Cache[F, A, B] {
          def put(key: A, value: B) =
            for {
              _ <- cancelRemoval(key)
              _ <- ref.update(_ + (key -> Tuple2(value, None)))
            } yield ()

          def putWithExpiry(key: A, value: B, expireAfter: FiniteDuration) =
            for {
              _ <- cancelRemoval(key)
              removalTimer <- scheduleRemoval(key, expireAfter)
              _ <- ref.update(_ + (key -> Tuple2(value, Some(removalTimer))))
            } yield ()

          def get(key: A): F[Option[B]] = ref.get.map(_.get(key).map { case (value, _) => value })

          /** Schedule removal removal of a key */
          private def scheduleRemoval(key: A, removeAfter: FiniteDuration): F[Fiber[F, Unit]] =
            Concurrent[F].start {
              Timer[F].sleep(removeAfter) *> ref.update(_ - key)
            }

          /** Cancel any ongoing removal timer */
          private def cancelRemoval(key: A): F[Unit] = ref.get
            .map(_.get(key) match {
              case Some((_, Some(fiber))) => fiber.cancel
              case _                      => Sync[F].unit
            })
            .flatten
        }
    }
}
