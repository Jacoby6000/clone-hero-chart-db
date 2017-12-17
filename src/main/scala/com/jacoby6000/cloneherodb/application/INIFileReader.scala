package com.jacoby6000.cloneherodb.application

import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

import com.jacoby6000.cloneherodb.application.INIFileReader._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.{DatabaseFiles, DatabaseSongs}
import com.jacoby6000.cloneherodb.database.DatabaseFiles.{File => DatabaseFile}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.Song
import com.jacoby6000.cloneherodb.filesystem.FileSystem
import com.jacoby6000.cloneherodb.logging.Logger
import com.jacoby6000.cloneherodb.parsing.ini.parser._

import scalaz._
import Scalaz._
import scalaz.Maybe.{Empty, Just}

object INIFileReader {

  sealed trait INIFileReaderError { def widen: INIFileReaderError = this }
  case class FileRootNotFoundInDatabase(uuid: UUIDFor[File]) extends INIFileReaderError
  case class FileRootNotFoundInFilesystem(uuid: ApiKeyFor[File]) extends INIFileReaderError
  case class FailedToParseSongINI(uuid: UUIDFor[File], parseError: ParseError) extends INIFileReaderError
  case class FileDoesNotExistInFilesystem(uuid: UUIDFor[File]) extends INIFileReaderError
  case class FailedToSaveSong(cannotSaveSongError: CannotSaveSongError) extends INIFileReaderError

  sealed trait CannotSaveSongError { def widen: CannotSaveSongError = this }
  case class MissingSection(section: Maybe[INISectionName]) extends CannotSaveSongError
  case class MissingKey(section: Maybe[INISectionName], oneOf: IList[INIKey]) extends CannotSaveSongError

  type FileIdentifierPair = (UUIDFor[File], ApiKeyFor[File])
}

class INIFileReader[F[_], M[_], N[_]](
  fileDb: DatabaseFiles[M],
  songDb: DatabaseSongs[M],
  filesystemProvider: ApiKey => FileSystem[N],
  parseINIFile: String => ParseResult,
  logger: Logger[F])(
  mToF: M ~> F,
  nToF: N ~> F)(implicit
  F: MonadError[F, INIFileReaderError],
  N: Monad[N],
  M: Monad[M]
) {


  def parseRoot(uuid: UUIDFor[File]) =
    for {
      file <- mToF(fileDb.getFile(uuid)).liftEmpty[INIFileReaderError].apply {
        FileRootNotFoundInDatabase(uuid).pure[F]
      }

      maybeRootINIFile = file.justIf(_.fileType == FileType.INI).map(makeFileIdentifierPair(uuid, _))

      iniFiles <- mToF(getINIFilePaths(uuid))
      parsed <- nToF(readINIFiles(maybeRootINIFile.toIList ::: iniFiles))
      //insertSongResult <- mToF(songDb.insertSong))
    } yield iniFiles

  def getINIFilePaths(uuid: UUIDFor[File]): M[IList[FileIdentifierPair]] =
    fileDb.getChildren(uuid).flatMap(files => files.traverseM {
      case (id, DatabaseFile(_, _, _, FileType.Directory, _, _)) => getINIFilePaths(id)
      case (id, file @ DatabaseFile(_, _, _, FileType.INI|FileType.Chart, _, _)) => M.pure(IList(makeFileIdentifierPair(id, file)))
      case _ => M.pure(IList())
    })

  def makeFileIdentifierPair(id: UUIDFor[File], file: DatabaseFile): FileIdentifierPair = (id, file.apiKey)

  def readINIFiles(entries: IList[FileIdentifierPair]): N[IList[(UUIDFor[File], Maybe[ParseResult])]] =
    entries.traverse { case (id, key) => readINIFile(key).map((id, _)) }

  def readINIFile(key: ApiKeyFor[File]): N[Maybe[ParseResult]] = {
    val fs = filesystemProvider(key.value)

    for {
      maybeFile <- fs.fileAt(apiKeyToPath(key.value))
      textContents <- maybeFile.traverseM(fs.textContents(_))
    } yield textContents.map(parseINIFile)
  }


  def apiKeyToPath(key: ApiKey): FilePath =
    key.fold(k => filePath(PathPart(k)), k => filePath(Paths.get(k)))


  def makePathToApiKeyFunc(initialKey: ApiKey): FilePath => ApiKey =
    initialKey.fold(
      _ => filePath => GoogleApiKey(filePath.end.value),
      _ => filePath => LocalFSApiKey(filePath.javaPath.toString)
    )



  def handleParsedResults(results: IList[(UUIDFor[File], Maybe[ParseResult])]): IList[ValidationNel[INIFileReaderError, (UUIDFor[File], INIFile)]] =
    results.map {
      case (id, Empty()) => FileDoesNotExistInFilesystem(id).widen.failureNel
      case (id, Just(Success(file))) => (id, file).successNel
      case (id, Just(Failure(errs))) => errs.map(FailedToParseSongINI(id, _).widen).failure
    }

  def saveSongs(data: IList[(UUIDFor[File], INIFile)]): M[] =
    data.map((songFromINIData _).tupled).map(_.traverse(songDb.insertSong(UUID.randomUUID().asEntityId, _)))

  def songFromINIData(uuid: UUIDFor[File], ini: INIFile): ValidationNel[CannotSaveSongError, Song] = {
    val songSection   = INISectionName("song").just
    val songNameKeys  = IList(INIKey("Name"), INIKey("name"))
    val artistKeys    = IList(INIKey("Artist"), INIKey("artist"))
    val albumKeys     = IList(INIKey("Album"), INIKey("album"))
    val genreKeys     = IList(INIKey("genre"), INIKey("Genre"))
    val charterKeys   = IList(INIKey("Charter"), INIKey("frets"))
    val yearKeys      = IList(INIKey("Year"), INIKey("year"))
    val now = Instant.now()

    ini.lookup(songSection).cata(
      iniSection => { val section = iniSection.map(_.value)
        (uuid.successNel[CannotSaveSongError] |@|
        section.lookupOneOf(songNameKeys).map(SongName(_)).toSuccess(MissingKey(songSection, songNameKeys)).toValidationNel |@|
        section.lookupOneOf(artistKeys).map(Artist(_)).successNel |@|
        section.lookupOneOf(albumKeys).map(Album(_)).successNel |@|
        section.lookupOneOf(genreKeys).map(Genre(_)).successNel |@|
        section.lookupOneOf(charterKeys).map(Charter(_)).successNel |@|
        section.lookupOneOf(yearKeys).map(Year(_)).successNel |@|
        now.successNel |@|
        now.successNel)(Song.apply _) },
      MissingSection(songSection).widen.failureNel
    )
  }

  // def storeINIData(ini)

}

