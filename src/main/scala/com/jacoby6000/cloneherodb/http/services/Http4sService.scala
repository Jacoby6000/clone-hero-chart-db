package com.jacoby6000.cloneherodb.http.services

import cats.effect._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpService
import com.jacoby6000.cloneherodb.data._
import java.util.UUID
import scala.util.Try

abstract class Http4sService[F[_]: Effect] extends Http4sDsl[F] {
  type Service = HttpService[F]
  val Service = HttpService[F](_)


  object UUIDForFileVar {
    def unapply(str: String): Option[UUIDFor[File]] =
      Try(UUID.fromString(str).asEntityId[File]).toOption
  }
}
