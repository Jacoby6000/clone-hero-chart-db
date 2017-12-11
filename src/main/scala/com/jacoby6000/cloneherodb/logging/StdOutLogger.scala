package com.jacoby6000.cloneherodb.logging

import scalaz.Monad

import scalaz.{ISet, Show}

class StdOutLogger[F[_]](showLogLevel: Show[LogLevel], levels: ISet[LogLevel])(implicit F: Monad[F]) extends Logger[F] {
  override def log[A](a: A, level: LogLevel)(implicit A: Show[A]): F[Unit] =
    if (levels.contains(level)) {
      F.pure(println("[" + showLogLevel.show(level) + "] " + A.show(a)))
    }
    else
      F.pure(())

}