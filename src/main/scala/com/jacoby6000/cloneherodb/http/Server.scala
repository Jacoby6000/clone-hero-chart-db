package com.jacoby6000.cloneherodb.http

import cats.effect.{Effect, IO}

import fs2.Stream
import org.http4s.server.blaze._
import org.http4s.util.StreamApp
import org.http4s.util.ExitCode

abstract class AbstractServer[F[_]: Effect] extends StreamApp[F] {
  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] =
    BlazeBuilder[F]
      .bindHttp(8080, "localhost")
      .mountService(Predef.???, "/api")
      .serve
}



object Server extends AbstractServer[IO]
