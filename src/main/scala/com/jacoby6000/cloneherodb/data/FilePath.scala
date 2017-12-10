package com.jacoby6000.cloneherodb.data

import java.nio.file.{Path, Paths}
import scalaz._, Scalaz._
import scalaz.Maybe.{Empty, Just}

class FilePathStringContextOps(val s: StringContext) extends AnyVal {
  def path(args: String*): PathPart = PathPart(s.s(args: _*))
}

case class PathPart(value: String) extends AnyVal {
  def /(pathPart: PathPart): FilePath = filePath(this, pathPart)
}

object PathPart {
  implicit val pathPartEq: Equal[PathPart] = Equal.equalA
}

case class FileName(value: String) extends AnyVal
object FileName {
  implicit val fileNameEq: Equal[FileName] = Equal.equalA
}

class FilePathOps(val path: FilePath) extends AnyVal {
  def beginning: PathPart = path.head
  def end: PathPart = path.tail.backMaybe.getOrElse(path.head)

  def dequeue: Dequeue[PathPart] = Dequeue(path.head) ++ path.tail

  def upDir: Maybe[FilePath] =
    path.tail.unsnoc.map { case (_, q) => filePath(path.head, q)}

  def /(part: PathPart): FilePath = /(filePath(part))
  def /(subPath: FilePath): FilePath = path |+| subPath

  def javaPath: Path = Paths.get(path.map(_.value).foldLeft(".")(_ + "/" + _))


  def containsSub(otherPath: FilePath): Boolean = {
    val otherDequeue = otherPath.dequeue
    def go(p: Dequeue[PathPart]): Boolean = {
      if (p === otherDequeue) true
      else p.uncons match {
        case Just((_, unconsed)) => go(unconsed)
        case Empty() => false
      }
    }

    go(otherPath.head +: otherPath.tail)
  }
}
