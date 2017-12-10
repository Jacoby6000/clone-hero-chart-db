package com.jacoby6000.cloneherodb.application.filesystem

import java.nio.file.{FileVisitOption, Files}

import cats.effect.Effect
import com.jacoby6000.cloneherodb.data
import com.jacoby6000.cloneherodb.data.{File, FilePath}
import shims._
import scala.collection.JavaConverters._
import scalaz._, Scalaz._

import scalaz.{IList, INil}

class LocalFilesystem[F[_]](implicit F: Effect[F]) extends FileSystem[F] {
  def childrenOf(filePath: FilePath): F[IList[File]] = F.delay {
    val javaPath = filePath.javaPath
    if (Files.isDirectory(javaPath)) {
      Files.walk(javaPath, 1).asScala.toIList
    } else INil()
  }


  def parentOf(filePath: FilePath) = ???
  def fileAt(filePath: FilePath) = ???
  def textContents(file: File) = ???
}
