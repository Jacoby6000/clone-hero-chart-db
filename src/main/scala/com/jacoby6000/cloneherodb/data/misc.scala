package com.jacoby6000.cloneherodb.data

import com.jacoby6000.cloneherodb.syntax._
import enumeratum._

import scalaz.Scalaz._
import scalaz.{Enum => _, _}

trait Song
trait INIEntry

case class SongName(value: String) extends AnyVal
object SongName {
  implicit val songNameEqual: Equal[SongName] = Equal.equalA
}

case class FileName(value: String) extends AnyVal
object FileName {
  implicit val fileNameEq: Equal[FileName] = Equal.equalA
}

case class Artist(value: String) extends AnyVal
case class Album(value: String) extends AnyVal
case class Genre(value: String) extends AnyVal
case class Charter(value: String) extends AnyVal
case class Year(value: String) extends AnyVal


sealed trait ApiKey {
  def fold[A](googleApiKeyF: String => A, localFSApiKeyF: String => A) =
    this match {
      case GoogleApiKey(s) => googleApiKeyF(s)
      case LocalFSApiKey(s) => localFSApiKeyF(s)
    }
}
case class GoogleApiKey(key: String) extends ApiKey
case class LocalFSApiKey(path: String) extends ApiKey
object ApiKey {
  implicit val showApiKey: Show[ApiKey] = Show.show[ApiKey] {
    case GoogleApiKey(k) => s"google://$k"
    case LocalFSApiKey(k) => s"file://$k"
  }

  implicit val apiKeyEqual: Equal[ApiKey] = Equal.equalA

  def fromTypeAndKey(keyType: ApiKeyType, key: String): ApiKey =
    keyType match {
      case ApiKeyType.GoogleApiKey => GoogleApiKey(key)
      case ApiKeyType.LocalFSApiKey => LocalFSApiKey(key)
    }
}

sealed trait ApiKeyType extends EnumEntry
object ApiKeyType extends Enum[ApiKeyType] {
  case object GoogleApiKey extends ApiKeyType
  case object LocalFSApiKey extends ApiKeyType

  val values = findValues
}


case class EntityId[A, +B](value: B) extends AnyVal
object EntityId {
  implicit def entityIdEq[A, B]: Equal[EntityId[A, B]] = Equal.equalA
  implicit def entityIdShow[A, B: Show]: Show[EntityId[A, B]] = Show.show(id => show"EntityId(${id.value})".toString)
}

case class File(path: FilePath, name: FileName, fileSize: Maybe[Long], fileType: FileType)
object File {
  implicit val fileEq: Equal[File] = Equal.equalA
}

sealed trait FileType extends EnumEntry
object FileType extends Enum[FileType] {
  implicit val fileTypeEqual: Equal[FileType] = Equal.equalA

  sealed trait ImageFile extends FileType
  sealed trait AudioFile extends FileType

  case object Directory extends FileType
  case object Midi extends FileType
  case object Chart extends FileType
  case object INI extends FileType
  case object Ogg extends AudioFile
  case object PNG extends ImageFile
  case object JPG extends ImageFile
  case class Unknown(fileType: String) extends FileType

  def values = findValues
}

case class PathPart(value: String) extends AnyVal {
  def /(pathPart: PathPart): FilePath = filePath(this, pathPart)
}

object PathPart {
  implicit val pathPartEq: Equal[PathPart] = Equal.equalA
}
