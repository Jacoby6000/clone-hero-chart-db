package com.jacoby6000.cloneherodb.data

import enumeratum._

import scalaz.{Enum => _, _}
import Scalaz._
import scalaz.Maybe.Empty

case class SongName(value: String) extends AnyVal
object SongName {
  implicit val songNameEqual: Equal[SongName] = Equal.equalA
}

case class DirectoryName(value: String) extends AnyVal
object DirectoryName {
  implicit val directoryNameEqual: Equal[DirectoryName] = Equal.equalA
}

sealed trait ApiKey
case class GoogleApiKey(key: String) extends ApiKey
object ApiKey {
  implicit val apiKeyEqual: Equal[ApiKey] = Equal.equalA
}

case class EntityId[A, B](value: B) extends AnyVal
object EntityId {
  implicit def entityIdEq[A, B]: Equal[EntityId[A, B]] = Equal.equalA
}


class AllOps[A](val a: A) extends AnyVal {
  def asEntityId[B]: EntityId[B, A] = EntityId(a)

  def justIf(f: A => Boolean): Maybe[A] = if (f(a)) a.just else Empty()
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
  case object INI extends FileType
  case object Ogg extends AudioFile
  case object PNG extends ImageFile
  case object JPG extends ImageFile
  case class Unknown(fileType: String) extends FileType

  def values = findValues
}
