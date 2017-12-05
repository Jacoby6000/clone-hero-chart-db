package com.jacoby6000.cloneherodb.application

import com.jacoby6000.cloneherodb.data._

trait FileSystem[F[_]] {

  def childrenOf(filePath: FilePath): F[List[File]]
  def parentOf(filePath: FilePath): F[Option[File]]
  def fileAt(filePath: FilePath): F[Option[File]]
  def textContents(file: File): F[Option[String]]
}
