package com.jacoby6000.cloneherodb.http

import cats.effect.{Effect, IO}
import com.jacoby6000.cloneherodb.application.Indexer.IndexerError
import com.jacoby6000.cloneherodb.application.IndexerImpl
import com.jacoby6000.cloneherodb.application.filesystem.FileSystem
import com.jacoby6000.cloneherodb.database.DatabaseSongs
import com.jacoby6000.cloneherodb.http.services.IndexerService
import fs2.Stream
import org.http4s.server.blaze._
import org.http4s.util.StreamApp
import org.http4s.util.ExitCode
import shims._

import scalaz.~>
import scalaz._

abstract class AbstractServer[F[_]: Effect] extends StreamApp[F] {
  type G[A] = EitherT[F, IndexerError, A]
  val dbSongs = new DatabaseSongs[G] {
    def getFile(id: com.jacoby6000.cloneherodb.data.UUIDFor[com.jacoby6000.cloneherodb.data.File]): AbstractServer.this.G[Maybe[com.jacoby6000.cloneherodb.database.Songs.File]] = ???
    def insertFile(id: com.jacoby6000.cloneherodb.data.UUIDFor[com.jacoby6000.cloneherodb.data.File],file: com.jacoby6000.cloneherodb.database.Songs.File): AbstractServer.this.G[Unit] = ???
    def updateFile(id: com.jacoby6000.cloneherodb.data.UUIDFor[com.jacoby6000.cloneherodb.data.File],file: com.jacoby6000.cloneherodb.database.Songs.File): AbstractServer.this.G[Boolean] = ???
    def updateFileByApiKey(file: com.jacoby6000.cloneherodb.database.Songs.File): scalaz.EitherT[AbstractServer.this.G,com.jacoby6000.cloneherodb.data.UUIDFor[com.jacoby6000.cloneherodb.data.File] => AbstractServer.this.G[Unit],Unit] = ???
  }
  val fs = new FileSystem[G] {
    def childrenOf(filePath: com.jacoby6000.cloneherodb.data.FilePath): AbstractServer.this.G[IList[com.jacoby6000.cloneherodb.data.File]] = ???
    def fileAt(filePath: com.jacoby6000.cloneherodb.data.FilePath): AbstractServer.this.G[Maybe[com.jacoby6000.cloneherodb.data.File]] = ???
    def parentOf(filePath: com.jacoby6000.cloneherodb.data.FilePath): AbstractServer.this.G[Maybe[com.jacoby6000.cloneherodb.data.File]] = ???
    def textContents(file: com.jacoby6000.cloneherodb.data.File): AbstractServer.this.G[Maybe[String]] = ???
  }

  def fuck = new (G ~> F) {
    def apply[A](fa: G[A]): F[A] = ???
  }

  def ident[M[_]] = new (M ~> M) {
    def apply[A](fa: M[A]): M[A] = ???
  }

  val service = new IndexerService[F, G](
    new IndexerImpl(dbSongs, fs)(ident, ident),
    fuck
  )

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] =
    BlazeBuilder[F]
      .bindHttp(8080, "localhost")
      .mountService(service.service, "/api")
      .serve
}



object Server extends AbstractServer[IO]
