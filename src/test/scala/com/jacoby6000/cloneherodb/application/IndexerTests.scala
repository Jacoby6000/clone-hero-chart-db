package com.jacoby6000.cloneherodb.application

import cats.effect.IO
import org.scalacheck._
import FileSystem._
import Indexer._

object IndexerSpecification extends Properties("Indexer") {
  def filesystem(files: FileTree) = new FileSystem[IO] {
    def childrenOf(filePath: FilePath): IO[List[File]] = IO {
      files
        .findByPath(filePath)
        .map(_.fold[List[File]]((_, children) => children.flatten, List(_)))
        .getOrElse(List.empty)
    }

    def fileAt(filePath: FilePath): IO[Option[File]] = IO {
      files
        .findByPath(filePath)
        .map(_.fold[File]((parent, _) => parent, identity))
    }


    // unused by indexer
    def parentOf(filePath: FilePath): IO[Option[File]] = ???
    def textContents(file: File): IO[Option[String]] = ???
  }

  val pathArbitrary: Gen[FilePath] =
    for {
      depth <- Gen.posNum[Int].map(_ % 5).map(_ + 1)
      parts <- Gen.listOfN(depth, Gen.alphaStr).map(_.map(PathPart(_)))
    } yield parts.tail.foldLeft(PathStart(parts.head).widen)((path, part) => PathSegment(path, part))
}
