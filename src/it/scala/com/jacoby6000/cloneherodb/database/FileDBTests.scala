package com.jacoby6000.cloneherodb.database

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.logging.{LogLevel, Logger}
import doobie._
import doobie.free.connection
import doobie.scalatest.IOChecker
import org.scalatest.{FunSuite, Matchers}

import scalaz.Maybe.Empty
import scalaz._

class FileDBTests extends FunSuite with Matchers with IOChecker {
  val logger = new Logger[ConnectionIO] {
    override def log[A: Show](a: A, level: LogLevel): ConnectionIO[Unit] =
      connection.unit
  }

  val db = new DoobieDatabaseSongs(logger)

  val conf =
    loadCloneHeroDbConfig((path"src" / path"it" /  path"resources" / path"reference.conf").javaPath)
      .fold(errs => sys.error(failuresToErrorMessage(errs)), identity)

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
  val file = com.jacoby6000.cloneherodb.database.Songs.File(
    FileName("foo"),
    GoogleApiKey("bar").asEntityId,
    Empty(),
    FileType.INI,
    Instant.now,
    Instant.now
  )

  test("DoobieDatabaseFiles.getFileQuery") { check(db.getFileQuery(uuid))}
  test("DoobieDatabaseFiles.getFileByApiKeyQuery") { check(db.getFileByApiKeyQuery(file.apiKey)) }
  test("DoobieDatabaseFiles.insertFileQuery") { check(db.insertFileQuery(uuid, file)) }
  test("DoobieDatabaseFiles.updateFileQuery") { check(db.updateFileQuery(uuid, file)) }
  test("DoobieDatabaseFiles.updateFileByApiKeyQuery") { check(db.updateFileByApiKeyQuery(file)) }
}
