package com.jacoby6000.cloneherodb.application.filesystem

import cats.effect.Effect
import com.jacoby6000.cloneherodb.data
import com.jacoby6000.cloneherodb.data.{File, FilePath}
import shims._

class LocalFilesystem[F[_]](implicit F: Effect[F]) extends FileSystem[F] {
  def childrenOf(filePath: FilePath) = F.delay()
  def parentOf(filePath: FilePath) = ???
  def fileAt(filePath: FilePath) = ???
  def textContents(file: File) = ???
}
