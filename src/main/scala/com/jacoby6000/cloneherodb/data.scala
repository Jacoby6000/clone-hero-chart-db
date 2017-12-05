package com.jacoby6000.cloneherodb

import java.util.UUID
import enumeratum._
import scalaz.{Enum => _, _}, Scalaz._

object data {
  case class SongName(value: String) extends AnyVal
  case class DirectoryName(value: String) extends AnyVal


  sealed trait ApiKey
  case class GoogleApiKey(key: String) extends ApiKey

  case class EntityId[A, B](value: B) extends AnyVal
  type UUIDFor[A] = EntityId[A, UUID]
  type ApiKeyFor[A] = EntityId[A, ApiKey]


  implicit class AllOps[A](val a: A) extends AnyVal {
    def asEntityId[B]: EntityId[B, A] = EntityId(a)
  }


  implicit class FileSystemStringContext(val s: StringContext) extends AnyVal {
    def path(args: String*): PathPart = PathPart(s.s(args: _*))
  }

  case class PathPart(value: String) extends AnyVal {
    def /(pathPart: PathPart): PathSegment = PathSegment(PathStart(this), pathPart)
  }
  case class FileName(value: String) extends AnyVal

  sealed trait FilePath {
    def widen: FilePath = this

    def contains(path: FilePath): Boolean =
      if(path == this) true else {
        path match {
          case PathStart(_) => false
          case PathSegment(parent, _) => this.contains(parent)
        }
      }

    def fold[A](f: PathPart => A, g: (FilePath, PathPart) => A): A =
      this match {
        case PathStart(path) => f(path)
        case PathSegment(parent, child) => g(parent, child)
      }

    def child: PathPart = fold(a => a, (_, a) => a)

    def parentOption: Option[FilePath] =
      fold(_ => none, (seg, _) => seg.some)

    def /(pathPart: PathPart): PathSegment = PathSegment(this, pathPart)

    def prepend(path: FilePath): FilePath =
      fold(
        PathSegment(path, _),
        (parent, child) => PathSegment(parent.prepend(path), child)
      )

    def append(path: FilePath): FilePath =
      path.prepend(this)
  }

  case class PathStart(startPart: PathPart) extends FilePath
  case class PathSegment(parent: FilePath, childPath: PathPart) extends FilePath

  case class File(path: FilePath, name: FileName, fileSize: Option[Int], fileType: FileType)

  sealed trait FileType extends EnumEntry
  object FileType extends Enum[FileType] {
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
}
