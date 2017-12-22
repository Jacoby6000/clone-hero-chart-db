package com.jacoby6000.cloneherodb.logging

import com.jacoby6000.cloneherodb.syntax.Shows

import scalaz.Monad
import scalaz.{ISet, Show}

class StdOutLogger[F[_]](showLogLevel: Show[LogLevel], levels: ISet[LogLevel])(implicit F: Monad[F]) extends Logger[F] {
  override def log(a: Shows, level: LogLevel): F[Unit] =
    if (levels.contains(level)) {
      F.pure(println("[" + showLogLevel.show(level) + "] " + a.toString))
    }
    else
      F.pure(())

}