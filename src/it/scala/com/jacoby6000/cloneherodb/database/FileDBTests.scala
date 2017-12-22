package com.jacoby6000.cloneherodb.database

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.logging.{LogLevel, Logger}
import doobie._
import doobie.scalatest.IOChecker
import org.scalatest.{FunSuite, Matchers}
import com.jacoby6000.cloneherodb.syntax._

import scalaz.Applicative
import scalaz.Maybe.Empty

class FileDBTests extends FunSuite with Matchers with IOChecker {
  val logger = new Logger {
    override def log[F[_]](a: Shows, level: LogLevel)(implicit F: Applicative[F]): F[Unit] =
      F.point(())

  }

  val db = new DoobieDatabaseFiles(logger)

  val conf =
    loadCloneHeroDbConfig((path"src" / path"it" /  path"resources" / path"reference.conf").javaPath)
      .fold(errs => sys.error(failuresToErrorMessage(errs)), identity(_))

  val driver = "org.postgresql.Driver"
  val connectionString = s"jdbc:postgresql://${conf.database.host}:${conf.database.port}/${conf.database.databaseName}"

  val transactor =
    Transactor.fromDriverManager[IO](
      driver,
      connectionString,
      conf.database.username,
      conf.database.password.getOrElse("")
    )

  val uuid = UUID.randomUUID().asEntityId[File]
  val file = com.jacoby6000.cloneherodb.database.DatabaseFiles.File(
    FileName("foo"),
    GoogleApiKey("bar").asEntityId,
    Empty(),
    FileType.INI,
    Instant.now,
    Instant.now
  )

  test("DoobieDatabaseFiles.getFileQuery") { check(db.getFileQuery(uuid))}
  test("DoobieDatabaseFiles.getChildrenQuery") { check(db.getChildrenQuery(uuid))}
  test("DoobieDatabaseFiles.getFileByApiKeyQuery") { check(db.getFileByApiKeyQuery(file.apiKey)) }
  test("DoobieDatabaseFiles.insertFileQuery") { check(db.insertFileQuery(uuid, file)) }
  test("DoobieDatabaseFiles.updateFileQuery") { check(db.updateFileQuery(uuid, file)) }
  test("DoobieDatabaseFiles.updateFileByApiKeyQuery") { check(db.updateFileByApiKeyQuery(file)) }
}
