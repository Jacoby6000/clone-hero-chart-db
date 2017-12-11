package com.jacoby6000.cloneherodb.http.services

import argonaut._
import cats.effect.Effect
import cats.implicits._
import com.jacoby6000.cloneherodb.application.Indexer
import com.jacoby6000.cloneherodb.data.{ApiKeyFor, File}
import com.jacoby6000.cloneherodb.http.json.data.codecs._
import com.jacoby6000.cloneherodb.http.services.IndexerService._
import com.jacoby6000.cloneherodb.logging.Logger

import scalaz._, Scalaz._

object IndexerService {
  case class NewFilesystemRoot(apiKey: ApiKeyFor[File])
  object NewFilesystemRoot {
    implicit val newFilesystemRootDecodeJson: DecodeJson[NewFilesystemRoot] =
      DecodeJson.jdecode1L(NewFilesystemRoot.apply)("apiKey")
  }

  case class ResourceIdResponse[A](id: A)
  object ResourceIdResponse {
    implicit def resourceIdResponseEncodeJson[A](implicit encoder: EncodeJson[A]): EncodeJson[ResourceIdResponse[A]] =
      EncodeJson.jencode1L[ResourceIdResponse[A], A](_.id)("id")
  }
}

class IndexerService[F[_] : Effect, G[_]](indexer: Indexer[G], nt: G ~> F, logger: Logger[F]) extends Http4sService[F] {

  val IndexerRoot = Root / "indexer"

  val service: Service = Service {
    case POST -> IndexerRoot / "re-index" / UUIDForFileVar(uuid) =>
      logger.verbose("Recieved re-index root request.")
      nt(indexer.index(uuid)).flatMap(files => Ok(files.map(_.apiKey.value)))

    case req @ POST -> IndexerRoot =>
      logger.verbose("Recieved new index root request.")
      req.decode[NewFilesystemRoot] { root =>
        logger.verbose("Successfully decoded new filesystem root: " + root.apiKey.value.show)
        nt(indexer.newIndex(root.apiKey)).map(ResourceIdResponse(_)).flatMap(Ok(_))
      }
  }
}
