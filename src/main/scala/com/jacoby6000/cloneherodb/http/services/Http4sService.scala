package com.jacoby6000.cloneherodb.http.services

import cats.effect._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpService}
import org.http4s.argonaut._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import java.util.UUID

import argonaut.{DecodeJson, EncodeJson}

import scala.Option
import scala.util.Try

abstract class Http4sService[F[_]: Effect] extends Http4sDsl[F] {
  type Service = HttpService[F]
  val Service = HttpService[F] _

  val errorLogger = HttpService

  object UUIDForFileVar {
    def unapply(str: String): Option[UUIDFor[File]] =
      Try(UUID.fromString(str).asEntityId[File]).toOption
  }

  implicit def http4sDecoder[A: DecodeJson]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def http4sEncoder[A: EncodeJson]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
