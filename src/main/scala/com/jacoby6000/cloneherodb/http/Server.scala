package com.jacoby6000.cloneherodb.http

import cats.effect.{Async, Effect, IO}
import com.jacoby6000.cloneherodb.application.FileSystemIndexer.IndexerError
import com.jacoby6000.cloneherodb.application.filesystem.{FileSystem, LocalFilesystem}
import com.jacoby6000.cloneherodb.application.{FileSystemIndexer, FileSystemIndexerImpl}
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data._
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

import scalaz.{~>, _}
import Scalaz._


abstract class AbstractServer[F[_]](implicit F: Effect[F]) extends StreamApp[F] {
  type G[A] = EitherT[F, IndexerError, A]

  val serviceLogger = new StdOutLogger[F](Show[LogLevel], ISet.fromList(LogLevel.values.toList))
  val indexerLogger = new StdOutLogger[G](Show[LogLevel], ISet.fromList(LogLevel.values.toList))
  val doobieLogger  = new StdOutLogger[ConnectionIO](Show[LogLevel], ISet.fromList(LogLevel.values.toList))

  def eitherTAsync[M[_], E](implicit M: Async[M]): Async[EitherT[M, E, ?]] =
    new Async[EitherT[M, E, ?]] {
      override def async[A](k: (Either[Throwable, A] => Unit) => Unit): EitherT[M, E, A] =
        EitherT.right(M.async(k))

      override def suspend[A](thunk: => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.suspend(M.pure(thunk))).flatMap(identity)

      override def flatMap[A, B](fa: EitherT[M, E, A])(f: A => EitherT[M, E, B]): EitherT[M, E, B] =
        fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A => EitherT[M, E, Either[A, B]]): EitherT[M, E, B] =
        BindRec[EitherT[M, E, ?]].tailrecM(f andThen (_.map(_.disjunction)))(a)

      override def raiseError[A](e: Throwable): EitherT[M, E, A] =
        EitherT.right(M.raiseError(e))

      override def handleErrorWith[A](fa: EitherT[M, E, A])(f: Throwable => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.handleErrorWith(M.pure(fa))(f andThen M.pure)).flatMap(identity)

      override def pure[A](x: A): EitherT[M, E, A] = EitherT.right(M.pure(x))
    }

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    implicit def eitherTAsyncImplicit[M[_]: Async, E]: Async[EitherT[M, E, ?]] = eitherTAsync

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
          .bindHttp(8080, "localhost")
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
