package com.jacoby6000.cloneherodb.database

import java.time.Instant

import com.jacoby6000.cloneherodb.data.{File => DataFile, _}
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.meta._
import com.jacoby6000.cloneherodb.database.syntax._
import com.jacoby6000.cloneherodb.logging.Logger
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import shims._


import scalaz.Scalaz._
import scalaz._

object DatabaseFiles {
  case class File(
    name: FileName,
    apiKey: ApiKeyFor[DataFile],
    parentId: Maybe[UUIDFor[DataFile]],
    fileType: FileType,
    lastIndexed: Instant,
    firstIndexed: Instant
  )
  object File {
    implicit val databaseFileShow: Show[File] = Show.showFromToString
  }
}

trait DatabaseFiles[F[_]] {
  import DatabaseFiles._

  def getFile(id: UUIDFor[DataFile]): F[Maybe[File]]
  def getFileByApiKey(apiKey: ApiKeyFor[DataFile]): F[Maybe[(UUIDFor[DataFile], File)]]
  def getChildren(id: UUIDFor[DataFile]): F[IList[(UUIDFor[DataFile], File)]]


  def insertFile(id: UUIDFor[DataFile], file: File): F[Unit]
  def updateFile(id: UUIDFor[DataFile], file: File): F[Boolean]

  def updateFileByApiKey(file: File): F[Maybe[(UUIDFor[DataFile], File)]]
}

class DoobieDatabaseFiles(val log: Logger) extends DatabaseFiles[ConnectionIO] {
  import DatabaseFiles._

  val logger = log.forF[ConnectionIO]

  def getFile(id: UUIDFor[DataFile]): ConnectionIO[Maybe[File]] =
    getFileQuery(id).maybe

  def getFileQuery(id: UUIDFor[DataFile]): Query0[File] =
    sql"""SELECT name, api_key, parent_id, file_type, last_indexed, first_indexed
          FROM files
          WHERE id = $id""".query[File]


  def getChildren(id: UUIDFor[DataFile]): ConnectionIO[IList[(UUIDFor[DataFile], File)]] =
    getChildrenQuery(id).iList

  def getChildrenQuery(id: UUIDFor[DataFile]): Query0[(UUIDFor[DataFile], File)] =
    sql"""SELECT id, name, api_key, parent_id, file_type, last_indexed, first_indexed
          FROM files
          WHERE parent_id = $id""".query[(UUIDFor[DataFile], File)]

  def insertFile(id: UUIDFor[DataFile], file: File): ConnectionIO[Unit] =
    insertFileQuery(id, file).runUnit

  def insertFileQuery(id: UUIDFor[DataFile], file: File): Update0 =
    sql"""INSERT INTO files (id, name, api_key, parent_id, file_type, last_indexed, first_indexed) VALUES (
      $id,
      ${file.name},
      ${file.apiKey},
      ${file.parentId},
      ${file.fileType},
      ${file.lastIndexed},
      ${file.firstIndexed}
    )""".update

  def updateFile(id: UUIDFor[DataFile], file: File): ConnectionIO[Boolean] =
    updateFileQuery(id, file).run.map(_ > 0) <* logger.debug(show"Inserted $file")

  def updateFileQuery(id: UUIDFor[DataFile], file: File): Update0 =
    sql"""UPDATE files SET (name, api_key, parent_id, file_type, last_indexed) = (
        ${file.name},
        ${file.apiKey},
        ${file.parentId},
        ${file.fileType},
        ${file.lastIndexed}
      ) WHERE id = $id""".update

  def getFileByApiKey(apiKey: ApiKeyFor[DataFile]): ConnectionIO[Maybe[(UUIDFor[DataFile], File)]] =
    getFileByApiKeyQuery(apiKey).maybe

  def getFileByApiKeyQuery(apiKey: ApiKeyFor[DataFile]): Query0[(UUIDFor[DataFile], File)] =
    sql"""SELECT id, name, api_key, parent_id, file_type, last_indexed, first_indexed
          FROM files
          WHERE api_key = $apiKey""".query[(UUIDFor[DataFile], File)]


  def updateFileByApiKey(file: File): ConnectionIO[Maybe[(UUIDFor[DataFile], File)]] = {
    for {
      found <- MaybeT(getFileByApiKey(file.apiKey))
      _ <- MaybeT(updateFileByApiKeyQuery(file).run.map(_.just))
      } yield found
    }.run

  def updateFileByApiKeyQuery(file: File): Update0 =
    sql"""UPDATE files SET (name, parent_id, file_type, last_indexed) = (
          ${file.name},
          ${file.parentId},
          ${file.fileType},
          ${file.lastIndexed}
        ) WHERE api_key = ${file.apiKey}""".update


}
