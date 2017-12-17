package com.jacoby6000.cloneherodb.filesystem

import better.files.{File => SystemFile}
import cats.effect.Effect
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.logging.Logger
import shims._

import scalaz.Scalaz._
import scalaz.{IList, _}

class LocalFilesystem[F[_]](logger: Logger[F])(implicit F: Effect[F]) extends FileSystem[F] {
  private def delay[A](a: => A): F[A] =
    F.handleErrorWith(F.delay(a))(ex => logger.error(ex.getMessage).flatMap(_ => F.raiseError(ex)))

  def childrenOf(filePath: FilePath): F[IList[File]] =
    delay(
        SystemFile(filePath.javaPath)
          .children
          .toList
          .toIList
          .flatMap(systemFileToFile(_).toIList)
    )

  def parentOf(filePath: FilePath): F[Maybe[File]] =
     delay(Maybe.fromOption(SystemFile(filePath.javaPath).parentOption).flatMap(systemFileToFile))


  def fileAt(filePath: FilePath): F[Maybe[File]] = delay {
    systemFileToFile(SystemFile(filePath.javaPath))
  }

  def textContents(file: File): F[Maybe[String]] = delay {
    SystemFile(file.path.javaPath).justIf(_.exists).map(_.contentAsString)
  }

  def systemFileToFile(file: SystemFile): Maybe[File] =
    file.justIf(_.exists).map { file =>
      File(
        filePath(file.path),
        FileName(file.name),
        file.size.just,
        if (file.isDirectory) FileType.Directory
        else file.extension.map(_.toLowerCase).map {
          case ".mid" => FileType.Midi
          case ".chart" => FileType.Chart
          case ".ini" => FileType.INI
          case ".ogg" => FileType.Ogg
          case ".png" => FileType.PNG
          case ".jpg" => FileType.JPG
          case unknown => FileType.Unknown(unknown.stripMargin('.'))
        }.getOrElse(FileType.Unknown(""))
      )
    }

}
