package com.jacoby6000.cloneherodb

import java.nio.file.Path
import java.util.UUID

import scalaz.Maybe.Empty
import scalaz.Scalaz._
import scalaz._

package object data {
  type UUIDFor[A] = EntityId[A, UUID]
  type ApiKeyFor[A] = EntityId[A, ApiKey]

  type FilePath = OneAnd[Dequeue, PathPart]

  def filePath(pathPart: PathPart): FilePath = OneAnd(pathPart, Dequeue.empty)
  def filePath(pathPart: PathPart, tail: PathPart*): FilePath = OneAnd(pathPart, Dequeue(tail: _*))
  def filePath(pathPart: PathPart, tail: Dequeue[PathPart]): FilePath = OneAnd(pathPart, tail)

  def empty[A]: Maybe[A] = Empty()

  def filePath(path: Path): FilePath = {
    val listToPath =
      NonEmptyList.lift[PathPart, FilePath](lst => lst.tail.foldLeft(filePath(lst.head))(_ / _))

    listToPath(
      path
        .toString
        .split(Array('/', '\\'))
        .map(PathPart(_))
        .toList
        .toIList
    ).getOrElse(filePath(PathPart(".")))
  }

  implicit def toFilePathOps(filePath: FilePath): FilePathOps =
    new FilePathOps(filePath)

  implicit def toFilePathStringContextOps(filePath: StringContext): FilePathStringContextOps =
    new FilePathStringContextOps(filePath)

  implicit def allOps[A](a: A): AllOps[A] = new AllOps(a)

  implicit val dequeueMonad: MonadPlus[Dequeue] =
    new MonadPlus[Dequeue] {
      override def point[A](a: => A): Dequeue[A] = Dequeue(a)
      override def bind[A, B](fa: Dequeue[A])(f: (A) => Dequeue[B]) = fa.foldMap(f)
      override def empty[A] = Dequeue.empty[A]
      override def plus[A](a: Dequeue[A], b: => Dequeue[A]) = a ++ b
    }
}
