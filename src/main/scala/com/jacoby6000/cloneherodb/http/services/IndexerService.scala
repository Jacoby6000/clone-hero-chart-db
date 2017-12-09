package com.jacoby6000.cloneherodb.http.services

import cats.effect.Effect
import cats.implicits._
import shims._
import com.jacoby6000.cloneherodb.application.Indexer
import scalaz.~>

class IndexerService[F[_]: Effect, G[_]](indexer: Indexer[G], nt: G ~> F) extends Http4sService[F] {

  val IndexerRoot = Root / "indexer"

  val service: Service = Service {
    case POST -> IndexerRoot / "re-index" / UUIDForFileVar(uuid) =>
      nt(indexer.index(uuid)).flatMap(result => Ok(result.toString))
  }
}
