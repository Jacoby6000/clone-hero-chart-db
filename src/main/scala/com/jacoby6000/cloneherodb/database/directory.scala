package com.jacoby6000.cloneherodb.database

import com.jacoby6000.cloneherodb.data.{File => DataFile, _}
import java.time.Instant
import scalaz._, Scalaz._
import doobie._, doobie.implicits._
import doobie.postgres.implicits._
import meta._

object Songs {

  case class File(
    name: FileName,
    apiKey: ApiKeyFor[DataFile],
    parent: Maybe[UUIDFor[DataFile]],
    fileType: FileType,
    lastIndexed: Instant,
    firstIndexed: Instant
  )

  case class Charter(name: String, username: String)

  case class Song(
    name: SongName,
    directory: Maybe[UUIDFor[DataFile]],
    lastIndexed: Instant,
    firstIndexed: Instant
  )

}

trait DatabaseSongs[F[_]] {
  import Songs._
  def getFile(id: UUIDFor[DataFile]): F[Maybe[File]]

  def insertFile(id: UUIDFor[DataFile], file: File): F[Unit]
  def updateFile(id: UUIDFor[DataFile], file: File): F[Boolean]
  def updateFileByApiKey(file: File): EitherT[F, UUIDFor[DataFile] => F[Unit], Unit]
}

class DoobieDatabaseSongs extends DatabaseSongs[ConnectionIO] {
  import Songs._

  def getFile(id: UUIDFor[DataFile]): ConnectionIO[Maybe[File]] =
    sql"""SELECT (name, api_key, parent, file_type, last_indexed, first_indexed)
          FROM files
          WHERE id = $id""".query[File].maybe

  def insertFile(id: UUIDFor[DataFile], file: File): ConnectionIO[Unit] =
    sql"""INSERT INTO files (id, name, api_key, parent, file_type, last_indexed, first_indexed) VALUES (
      $id,
      ${file.name},
      ${file.apiKey},
      ${file.parent},
      ${file.fileType},
      ${file.lastIndexed},
      ${file.firstIndexed}
    )""".update.run.map(_ => ())

  def updateFile(id: UUIDFor[DataFile], file: File): ConnectionIO[Boolean] =
    sql"""UPDATE files SET (name, api_key, parent, file_type, last_indexed) = (
        ${file.name},
        ${file.apiKey},
        ${file.parent},
        ${file.fileType},
        ${file.lastIndexed}
      ) WHERE id = $id""".update.run.map(_ > 0)


  def updateFileByApiKey(file: File): EitherT[ConnectionIO, UUIDFor[DataFile] => ConnectionIO[Unit], Unit] = EitherT {
    sql"""UPDATE files SET (name, parent, file_type, last_indexed) = (
      ${file.name},
      ${file.parent},
      ${file.fileType},
      ${file.lastIndexed}
    ) WHERE api_key = ${file.apiKey}""".update.run.map {
      case 1 => ().right
      case _ => (insertFile(_: UUIDFor[DataFile], file)).left
    }
  }



}
