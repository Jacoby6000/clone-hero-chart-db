package com.jacoby6000.cloneherodb

import java.util.UUID
import scalaz._
import Scalaz._

package object data {
  type UUIDFor[A] = EntityId[A, UUID]
  type ApiKeyFor[A] = EntityId[A, ApiKey]

  type FilePath = OneAnd[Dequeue, PathPart]

  def filePath(pathPart: PathPart): FilePath = OneAnd(pathPart, Dequeue.empty)
  def filePath(pathPart: PathPart, tail: PathPart*): FilePath = OneAnd(pathPart, Dequeue(tail: _*))
  def filePath(pathPart: PathPart, tail: Dequeue[PathPart]): FilePath = OneAnd(pathPart, tail)

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
