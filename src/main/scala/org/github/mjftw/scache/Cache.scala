package org.github.mjftw.scache

import cats.effect.concurrent.Ref
import cats.effect.Sync
import cats.implicits._

trait Cache[F[_], A, B] {
  def put(key: A, value: B): F[Unit]
  def get(key: A): F[Option[B]]
}

object Cache {
  def make[F[_]: Sync, A, B]: F[Cache[F, A, B]] =
    Ref.of[F, Map[A, B]](Map.empty[A, B]).map { ref =>
      new Cache[F, A, B] {
        def put(key: A, value: B) = ref.update(_ + (key -> value))
        def get(key: A): F[Option[B]] = ref.get.map(_.get(key))
      }
    }
}