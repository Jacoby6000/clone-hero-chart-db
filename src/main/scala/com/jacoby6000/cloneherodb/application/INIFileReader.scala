package com.jacoby6000.cloneherodb.application

import java.nio.file.Paths

import com.jacoby6000.cloneherodb.application.INIFileReader._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.database.{DatabaseFiles, DatabaseSongs}
import com.jacoby6000.cloneherodb.database.DatabaseFiles.{File => DatabaseFile}
import com.jacoby6000.cloneherodb.filesystem.FileSystem
import com.jacoby6000.cloneherodb.logging.Logger
import com.jacoby6000.cloneherodb.parsing.ini.parser
import com.jacoby6000.cloneherodb.parsing.ini.parser.{INIFile, ParseError}

import scalaz._
import Scalaz._

object INIFileReader {

  sealed trait INIFileReaderError { def widen: INIFileReaderError = this }
  case class FileRootNotFoundInDatabase(uuid: UUIDFor[File]) extends INIFileReaderError
  case class FileRootNotFoundInFilesystem(uuid: ApiKeyFor[File]) extends INIFileReaderError
}

class INIFileReader[F[_], M[_], N[_]](
  fileDb: DatabaseFiles[M],
  songDb: DatabaseSongs[M],
  filesystemProvider: ApiKey => FileSystem[N],
  logger: Logger[F])(
  mToF: M ~> F,
  nToF: N ~> F)(implicit
  F: MonadError[F, INIFileReaderError],
  N: Monad[N],
  M: Monad[M]
) {

  /*
  def parseRoot(uuid: UUIDFor[File]) =
    for {
      file <- maybeToF(fileDb.getFile(uuid), mToF)(FileRootNotFoundInDatabase(uuid).widen)
      iniFiles <- mToF(getINIFilePaths(uuid))
      parsed <- (file.justIf(_.fileType == FileType.INI).toIList.map(file => (uuid, file.apiKey)) :: iniFiles).traverse(readI)
    } yield iniFiles

  def getINIFilePaths(uuid: UUIDFor[File]): M[IList[(UUIDFor[File], ApiKeyFor[File])]] =
    fileDb.getChildren(uuid).flatMap(files => files.traverseM {
      case (id, DatabaseFile(_, _, _, FileType.Directory, _, _)) => getINIFilePaths(id)
      case (id, file @ DatabaseFile(_, _, _, FileType.INI, _, _)) => M.pure(IList((id, file.apiKey)))
      case _ => M.pure(IList())
    })

  def readINIFile(key: ApiKeyFor[File]): N[Maybe[ValidationNel[ParseError, INIFile]]] = {
    val fs = filesystemProvider(key.value)

    fs.fileAt(apiKeyToPath(key.value))
      .flatMap(_.cata(fs.textContents(_), N.point(empty[String])))
      .map(_.map(parser.parse(_)))
  }


  def apiKeyToPath(key: ApiKey): FilePath =
    key.fold(k => filePath(PathPart(k)), k => filePath(Paths.get(k)))


  def makePathToApiKeyFunc(initialKey: ApiKey): FilePath => ApiKey =
    initialKey.fold(
      _ => filePath => GoogleApiKey(filePath.end.value),
      _ => filePath => LocalFSApiKey(filePath.javaPath.toString)
    )

  def maybeToF[G[_], A](maybeA: => G[Maybe[A]], nt: G ~> F)(raiseError: => INIFileReaderError): F[A] =
    for {
      maybeResult <- nt(maybeA)
      result <- maybeResult.getOrElseF[F](raiseAndLogError[A](raiseError))
    } yield result

  def validationToF[G[_], A, B](validationA: G[Validation[A, B]], nt: G ~> F)(raiseError: A => INIFileReaderError): F[B] =
    for {
      validationResult <- nt(validationA)
      result <- validationResult.fold(err => raiseAndLogError(raiseError(err)), F.pure(_))
    } yield result

  def raiseAndLogError[A](err: INIFileReaderError): F[A] =
    logger.error(err.toString) *> F.raiseError[A](err)
*/
}

