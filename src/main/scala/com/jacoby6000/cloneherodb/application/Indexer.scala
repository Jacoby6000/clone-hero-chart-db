package com.jacoby6000.cloneherodb.application

import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.database.DatabaseSongs
import com.jacoby6000.cloneherodb.http.clients.GoogleDrive

import scalaz._

object Indexer {
  case class Directory(parent: Option[Directory])
}

import Indexer._

trait Indexer[F[_]] {
  def index(id: UUIDFor[Directory]): F[Boolean]
}

class IndexerImpl[F[_], M[_], N[_]](songs: DatabaseSongs[M], googleDrive: GoogleDrive[N])(mToF: M ~> F, nToF: N ~> F) {

}
