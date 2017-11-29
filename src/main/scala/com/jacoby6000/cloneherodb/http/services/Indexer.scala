package com.jacoby6000.cloneherodb.http.services

import cats.effect.Effect
import shims._
import com.jacoby6000.cloneherodb.application.Indexer

class IndexerService[F[_]: Effect](indexer: Indexer[F]) extends Http4sService[F] {

  val IndexerRoot = Root / "indexer"

  val service: Service = Service {
    case POST -> IndexerRoot / "re-index" / _ =>
      Ok()
  }
}
