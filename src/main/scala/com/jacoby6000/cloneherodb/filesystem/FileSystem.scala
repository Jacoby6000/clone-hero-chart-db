package com.jacoby6000.cloneherodb.filesystem

import com.jacoby6000.cloneherodb.data._

import scalaz.{IList, Maybe}

trait FileSystem[F[_]] {
  def childrenOf(filePath: FilePath): F[IList[File]]
  def parentOf(filePath: FilePath): F[Maybe[File]]
  def fileAt(filePath: FilePath): F[Maybe[File]]
  def textContents(file: File): F[Maybe[String]]
}
