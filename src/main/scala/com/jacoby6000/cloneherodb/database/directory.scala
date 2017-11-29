package com.jacoby6000.cloneherodb.database

import com.jacoby6000.cloneherodb.data._
import java.time.Instant

object Songs {
  sealed trait File

  case class Directory(
    name: DirectoryName,
    apiKey: ApiKey,
    parent: Option[UUIDFor[Directory]],
    lastIndexed: Instant,
    firstIndexed: Instant
  ) extends File

  case class Song(
    name: SongName,
    apiKey: ApiKey,
    parent: Option[UUIDFor[Directory]],
    lastIndexed: Instant,
    firstIndexed: Instant
  ) extends File

}

trait DatabaseSongs[F[_]] {
  import Songs._
  def getDirectory(id: UUIDFor[Directory]): F[Option[Directory]]
  def getDirectoryChildren(id: UUIDFor[Directory]): F[List[Directory]]
  def getSongsInDirectory(id: UUIDFor[Directory]): F[List[Song]]
  def getFilesInDirectory(id: UUIDFor[Directory]): F[List[File]]
}
