package com.jacoby6000.cloneherodb.database

import com.jacoby6000.cloneherodb.application.FileSystem
import com.jacoby6000.cloneherodb.application.FileSystem._
import com.jacoby6000.cloneherodb.data._
import java.time.Instant
import scalaz._

object Songs {

  case class DatabaseFile(
    name: FileName,
    apiKey: ApiKeyFor[File],
    parent: Option[UUIDFor[File]],
    fileType: FileSystem.FileType,
    lastIndexed: Instant,
    firstIndexed: Instant
  )

  case class Song(
    name: SongName,
    directory: Option[UUIDFor[File]],
    lastIndexed: Instant,
    firstIndexed: Instant
  )

}

trait DatabaseSongs[F[_]] {
  import Songs._
  def getDatabaseFile(id: UUIDFor[File]): F[Option[DatabaseFile]]
  def getSong(id: UUIDFor[Song]): F[Option[Song]]

  def getDatabaseFileChildren(id: UUIDFor[File]): F[List[DatabaseFile]]
  def getSongsInDatabaseFile(id: UUIDFor[Song]): F[List[Song]]
  def getFilesInDatabaseFile(id: UUIDFor[File]): F[List[DatabaseFile]]

  def getFileByApiKey(key: ApiKeyFor[File]): F[Option[DatabaseFile]]
  def getDatabaseFileByApiKey(key: ApiKeyFor[File]): F[Option[DatabaseFile]]
  def getSongByApiKey(key: ApiKeyFor[File]): F[Option[Song]]

  def saveFile(id: UUIDFor[File], file: DatabaseFile): F[Unit]
  def updateFile(id: UUIDFor[File], file: DatabaseFile): F[Unit]
  def updateFileByApiKey(file: DatabaseFile): EitherT[F, UUIDFor[File] => F[Unit], Unit]
}

class DoobieDatabaseSongs()
