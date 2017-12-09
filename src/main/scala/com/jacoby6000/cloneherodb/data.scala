package com.jacoby6000.cloneherodb

import java.nio.file.{Path, Paths}
import java.util.UUID

import enumeratum._

import scalaz.{Enum => _, _}
import Scalaz._
import scalaz.Maybe.{Empty, Just}

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
    def /(pathPart: PathPart): FilePath = filePath(this, pathPart)
  }
  case class FileName(value: String) extends AnyVal

  implicit val dequeueMonad: MonadPlus[Dequeue] =
    new MonadPlus[Dequeue] {
      override def point[A](a: => A): Dequeue[A] = Dequeue(a)
      override def bind[A, B](fa: Dequeue[A])(f: (A) => Dequeue[B]) =
        fa.foldLeft(Dequeue.empty[B]) {
          case (acc, a) => acc ++ f(a)

        }
      override def empty[A] = Dequeue.empty[A]
      override def plus[A](a: Dequeue[A], b: => Dequeue[A]) = a ++ b
    }

  type FilePath = OneAnd[Dequeue, PathPart]

  def filePath(pathPart: PathPart): FilePath = OneAnd.oneAnd(pathPart, Dequeue.empty)
  def filePath(pathPart: PathPart, tail: PathPart*): FilePath = OneAnd(pathPart, Dequeue(tail: _*))
  def filePath(pathPart: PathPart, tail: Dequeue[PathPart]): FilePath = OneAnd(pathPart, tail)


  implicit class FilePathOps(val path: FilePath) extends AnyVal {
    def beginning: PathPart = path.head
    def end: PathPart = path.tail.backMaybe.getOrElse(path.head)

    def upDir: Maybe[FilePath] =
      path.tail.unsnoc.map { case (_, q) => filePath(path.head, q)}

    def /(part: PathPart): FilePath = /(filePath(part))
    def /(subPath: FilePath): FilePath = path |+| subPath

    def javaPath: Path = Paths.get(path.map(_.value).foldLeft(".")(_ + "/" + _))


    def containsSub(otherPath: FilePath): Boolean = {
      def go(p: Dequeue[PathPart]): Boolean = {
        if (p == otherPath) true
        else p.uncons match {
          case Just((_, unconsed)) => go(unconsed)
          case Empty() => false
        }
      }

      go(otherPath.head +: otherPath.tail)
    }
  }

  case class File(path: FilePath, name: FileName, fileSize: Maybe[Int], fileType: FileType)

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
