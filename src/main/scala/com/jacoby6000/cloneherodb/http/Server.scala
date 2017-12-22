package com.jacoby6000.cloneherodb.http

import cats.effect.{Effect, IO}
import com.jacoby6000.cloneherodb._
import com.jacoby6000.cloneherodb.application.FileSystemIndexer.FileIndexerError
import com.jacoby6000.cloneherodb.application.SongIndexer.SongIndexerError
import com.jacoby6000.cloneherodb.filesystem.{FileSystem, LocalFilesystem}
import com.jacoby6000.cloneherodb.application.{FileSystemIndexer, FileSystemIndexerImpl, SongIndexer, SongIndexerImpl}
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.{DoobieDatabaseFiles, DoobieDatabaseSongs}
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
  type G[A] = EitherT[F, FileIndexerError, A]
  type H[A] = EitherT[F, SongIndexerError, A]

  val logger = new StdOutLogger(Show[LogLevel], ISet.fromList(LogLevel.values.toList))

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    loadCloneHeroDbConfig((path"src" / path"main" / path"resources" / path"reference.conf").javaPath)
      .fold(fail, { conf =>

        val dbFiles = new DoobieDatabaseFiles(logger)
        val dbSongs = new DoobieDatabaseSongs
        val localFS = new LocalFilesystem[F](logger)
        // val googleDriveFS = ???

        val fsProvider: ApiKey => FileSystem[F] = {
          case LocalFSApiKey(_) => localFS
          case GoogleApiKey(_) =>  ???
        }

        // TODO: Require an error handler as input
        def fFromEitherT[E]: EitherT[F, E, ?] ~> F =
          new (EitherT[F, E, ?] ~> F) {
            def apply[A](fa: EitherT[F, E, A]): F[A] =
              F.handleErrorWith(fa.run.map(_.valueOr(err => throw new RuntimeException(err.toString)))) { err =>
                logger.error[F](err.getMessage) *>
                  logger.error[F](err.getStackTrace.mkString("\n")) *>
                  F.raiseError(err)
              }
          }


        def fToEitherT[E]: F ~> EitherT[F, E, ?] =
          new (F ~> EitherT[F, E, ?]) {
            override def apply[A](fa: F[A]) = EitherT.right(fa)
          }

        def ident[M[_]] = new (M ~> M) {
          def apply[A](fa: M[A]): M[A] = fa
        }

        val driver = "org.postgresql.Driver"
        val connectionString = s"jdbc:postgresql://${conf.database.host}:${conf.database.port}/${conf.database.databaseName}"

        val transactor =
          Transactor.fromDriverManager[F](
            driver,
            connectionString,
            conf.database.username,
            conf.database.password.getOrElse("")
          )

        val fileIndexer: FileSystemIndexer[G] = new FileSystemIndexerImpl(dbFiles, fsProvider, logger)(transactor.trans.asScalaz andThen fToEitherT, fToEitherT)
        val songIndexer: SongIndexer[H] = new SongIndexerImpl(dbFiles, dbSongs, fsProvider, com.jacoby6000.cloneherodb.parsing.chart.parser.parse(_), logger)(transactor.trans.asScalaz andThen fToEitherT, fToEitherT)

        val service = new IndexerService[F, G, H](
          fileIndexer,
          songIndexer,
          fFromEitherT,
          fFromEitherT,
          logger
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
