package com.jacoby6000.cloneherodb.http

import cats.effect.{Effect, IO}
import com.jacoby6000.cloneherodb._
import com.jacoby6000.cloneherodb.application.FileSystemIndexer.IndexerError
import com.jacoby6000.cloneherodb.filesystem.{FileSystem, LocalFilesystem}
import com.jacoby6000.cloneherodb.application.{FileSystemIndexer, FileSystemIndexerImpl}
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.DoobieDatabaseFiles
import com.jacoby6000.cloneherodb.http.services.IndexerService
import com.jacoby6000.cloneherodb.logging.{LogLevel, StdOutLogger}
import doobie._
import doobie.implicits._
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze._
import pureconfig.error.ConfigReaderFailures
import shims._

import scalaz._
import Scalaz._


abstract class AbstractServer[F[_]](implicit F: Effect[F]) extends StreamApp[F] {
  type G[A] = EitherT[F, IndexerError, A]

  val serviceLogger = new StdOutLogger[F](Show[LogLevel], ISet.fromList(LogLevel.values.toList))
  val indexerLogger = new StdOutLogger[G](Show[LogLevel], ISet.fromList(LogLevel.values.toList))
  val doobieLogger  = new StdOutLogger[ConnectionIO](Show[LogLevel], ISet.fromList(LogLevel.values.toList))

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    loadCloneHeroDbConfig((path"src" / path"main" / path"resources" / path"reference.conf").javaPath)
      .fold(fail, { conf =>

        val dbSongs = new DoobieDatabaseFiles(doobieLogger)
        val localFS = new LocalFilesystem[F](serviceLogger)
        // val googleDriveFS = ???

        val fsProvider: ApiKey => FileSystem[F] = {
          case LocalFSApiKey(_) => localFS
          case GoogleApiKey(_) =>  ???
        }

        def fuck = new (G ~> F) {
          def apply[A](fa: G[A]): F[A] =
            F.handleErrorWith(fa.run.map(_.valueOr(err => throw new RuntimeException(err.toString)))) { err =>
              serviceLogger.error(err.getMessage) *>
                serviceLogger.error(err.getStackTrace.mkString("\n")) *>
                F.raiseError(err)
            }
        }

        def fToG: F ~> G = new (F ~> G) {
          def apply[A](fa: F[A]): G[A] = EitherT.right(fa)
        }

        def ident[M[_]] = new (M ~> M) {
          def apply[A](fa: M[A]): M[A] = fa
        }

        val driver = "org.postgresql.Driver"
        val connectionString = s"jdbc:postgresql://${conf.database.host}:${conf.database.port}/${conf.database.databaseName}"

        val transactor =
          Transactor.fromDriverManager[G](
            driver,
            connectionString,
            conf.database.username,
            conf.database.password.getOrElse("")
          )

        val indexer: FileSystemIndexer[G] = new FileSystemIndexerImpl(dbSongs, fsProvider, indexerLogger)(transactor.trans.asScalaz, fToG)

        val service = new IndexerService[F, G](
          indexer,
          fuck,
          serviceLogger
        )


        BlazeBuilder[F]
          .bindHttp(conf.api.port, conf.api.bindTo)
          .mountService(service.service, "/api")
          .serve
      })
  }

  def fail(err: ConfigReaderFailures): Stream[F, ExitCode] = {
    println(failuresToErrorMessage(err))
    Stream(ExitCode.Error)
      .covary[F]
  }
}


object Server extends AbstractServer[IO]
