package com.jacoby6000.cloneherodb.syntax

import java.nio.file.{Path, Paths}

import com.jacoby6000.cloneherodb.data.{FilePath, filePath}

import scalaz.Scalaz._
import scalaz._

class FilePathStringContextOps(val s: StringContext) extends AnyVal {
  def path(args: String*): PathPart = PathPart(s.s(args: _*))
}

case class PathPart(value: String) extends AnyVal {
  def /(pathPart: PathPart): FilePath = filePath(this, pathPart)
}

object PathPart {
  implicit val pathPartEq: Equal[PathPart] = Equal.equalA
}

class FilePathOps(val path: FilePath) extends AnyVal {
  def beginning: PathPart = path.head

  def end: PathPart = path.tail.backMaybe.getOrElse(path.head)

  def dequeue: Dequeue[PathPart] = Dequeue(path.head) ++ path.tail

  def upDir: Maybe[FilePath] =
    path.tail.unsnoc.map { case (_, q) => filePath(path.head, q) }

  def /(part: PathPart): FilePath = /(filePath(part))

  def /(subPath: FilePath): FilePath = path |+| subPath

  def asString: String = path.tail.map(_.value).foldLeft(path.head.value)(_ + "/" + _)

  def javaPath: Path = Paths.get(asString)

  def containsSub(otherPath: FilePath): Boolean = {
    val otherDequeue = otherPath.dequeue

    def go(p: Dequeue[PathPart]): Boolean = {
      if (p === otherDequeue) true
      else p.uncons.cata({case (_, unconsed) => go(unconsed)}, false)
    }

    go(path.head +: path.tail)
  }
}
