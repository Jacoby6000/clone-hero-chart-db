package com.jacoby6000.cloneherodb

import java.nio.file.Path
import java.util.UUID

import syntax._

import scalaz.Scalaz._
import scalaz._

package object data {
  type UUIDFor[A] = EntityId[A, UUID]
  type ApiKeyFor[A] = EntityId[A, ApiKey]

  type FilePath = OneAnd[Dequeue, PathPart]

  def filePath(pathPart: PathPart): FilePath = OneAnd(pathPart, Dequeue.empty)
  def filePath(pathPart: PathPart, tail: PathPart*): FilePath = OneAnd(pathPart, Dequeue(tail: _*))
  def filePath(pathPart: PathPart, tail: Dequeue[PathPart]): FilePath = OneAnd(pathPart, tail)

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

  implicit def uuidShow: Show[UUID] = Show.show[UUID](uuid => s"{$uuid}")

  implicit def filePathShow: Show[FilePath] = Show.show[FilePath](path => path.asString)
}
