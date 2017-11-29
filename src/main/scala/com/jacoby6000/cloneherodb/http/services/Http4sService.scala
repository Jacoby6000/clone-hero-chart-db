package com.jacoby6000.cloneherodb.http.services

import cats.effect._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpService

abstract class Http4sService[F[_]: Effect] extends Http4sDsl[F] {
  type Service = HttpService[F]
  val Service = HttpService[F](_)
}
