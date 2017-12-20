package com.jacoby6000.cloneherodb.database

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import com.jacoby6000.cloneherodb.config._
import com.jacoby6000.cloneherodb.data.{Song => DataSong, _}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.Song
import com.jacoby6000.cloneherodb.syntax._
import doobie._
import doobie.scalatest.IOChecker
import org.scalatest.{FunSuite, Matchers}

import scalaz.Maybe.Empty

class SongDBTests extends FunSuite with Matchers with IOChecker {
  val db = new DoobieDatabaseSongs

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

  val songUUID = UUID.randomUUID().asEntityId[DataSong]
  val fileUUID = UUID.randomUUID().asEntityId[File]

  val song = Song(
    fileUUID,
    SongName("foo"),
    Empty(),
    Empty(),
    Empty(),
    Empty(),
    Empty(),
    Instant.now,
    Instant.now
  )

  test("DoobieDatabaseSongs.getSongByIdQuery") {check(db.getSongByIdQuery(songUUID))}
  test("DoobieDatabaseSongs.getSongByFileQuery") {check(db.getSongByFileQuery(fileUUID))}
  test("DoobieDatabaseSongs.updateSongByDataQuery") {check(db.updateSongByDataQuery(song))}
  test("DoobieDatabaseFiles.insertSongQuery") {check(db.insertSongQuery(songUUID, song))}
}
