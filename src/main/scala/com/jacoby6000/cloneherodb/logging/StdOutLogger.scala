package com.jacoby6000.cloneherodb.logging

import com.jacoby6000.cloneherodb.syntax.Shows

import scalaz.{Applicative, ISet, Show}

class StdOutLogger(showLogLevel: Show[LogLevel], levels: ISet[LogLevel]) extends Logger {
  override def log[F[_]](a: Shows, level: LogLevel)(implicit F: Applicative[F]): F[Unit] =
    if (levels.contains(level)) {
      F.pure(println("[" + showLogLevel.show(level) + "] " + a.toString))
    }
    else
      F.pure(())
}