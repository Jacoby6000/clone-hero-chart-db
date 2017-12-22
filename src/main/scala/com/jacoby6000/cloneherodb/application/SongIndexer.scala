package com.jacoby6000.cloneherodb.application

import scala.Predef.Set

import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

import com.jacoby6000.cloneherodb.application.SongIndexer._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.{DatabaseFiles, DatabaseSongs}
import com.jacoby6000.cloneherodb.database.DatabaseFiles.{File => DatabaseFile}
import com.jacoby6000.cloneherodb.database.DatabaseSongs.{Song => DatabaseSong}
import com.jacoby6000.cloneherodb.filesystem.FileSystem
import com.jacoby6000.cloneherodb.logging.Logger
import com.jacoby6000.cloneherodb.parsing.ini.parser._

import scalaz._
import Scalaz._
import scalaz.Maybe.{Empty, Just}

object SongIndexer {

  sealed trait SongIndexerError { def widen: SongIndexerError = this }
  case class FileRootNotFoundInDatabase(uuid: UUIDFor[File]) extends SongIndexerError
  case class FileRootNotFoundInFilesystem(uuid: ApiKeyFor[File]) extends SongIndexerError
  case class FailedToParseSongINI(uuid: UUIDFor[File], parseError: ParseError) extends SongIndexerError
  case class FileDoesNotExistInFilesystem(uuid: UUIDFor[File]) extends SongIndexerError
  case class FailedToSaveSong(cannotSaveSongError: CannotSaveSongError) extends SongIndexerError

  sealed trait CannotSaveSongError {
    def widen: CannotSaveSongError = this
    def iniFileReaderError: SongIndexerError = FailedToSaveSong(this)
  }
  case class MissingSection(sections: IList[Maybe[INISectionName]]) extends CannotSaveSongError
  case class MissingKey(section: IList[Maybe[INISectionName]], oneOf: IList[INIKey]) extends CannotSaveSongError

  type FileIdentifierPair = (UUIDFor[File], ApiKeyFor[File])
}

trait SongIndexer[F[_]] {
  def indexSongsAtRoot(uuid: UUIDFor[File]): F[IList[ValidationNel[SongIndexerError, UUIDFor[Song]]]]
}

class SongIndexerImpl[F[_], M[_], N[_]](
  fileDb: DatabaseFiles[M],
  songDb: DatabaseSongs[M],
  filesystemProvider: ApiKey => FileSystem[N],
  parseINIFile: String => ParseResult,
  logger: Logger[F])(
  mToF: M ~> F,
  nToF: N ~> F)(implicit
  F: MonadError[F, SongIndexerError],
  N: Monad[N],
  M: Monad[M]
) extends SongIndexer[F] {


  def indexSongsAtRoot(uuid: UUIDFor[File]): F[IList[ValidationNel[SongIndexerError, UUIDFor[Song]]]] =
    for {
      file <- mToF(fileDb.getFile(uuid)).liftEmpty[SongIndexerError].apply {
        FileRootNotFoundInDatabase(uuid).pure[F]
      }

      maybeRootINIFile = file.justIf(_.fileType == FileType.INI).map(makeFileIdentifierPair(uuid, _))

      iniFiles <- mToF(getINIFilePaths(uuid))
      parsed <- nToF(readINIFiles(maybeRootINIFile.toIList ::: iniFiles))
      handled = handleParsedResults(parsed)
      saveSongsResult <- mToF(handled.traverse(saveSongs(_)).map(_.map(_.fold(_.failure, identity))))
    } yield saveSongsResult

  def getINIFilePaths(uuid: UUIDFor[File]): M[IList[FileIdentifierPair]] =
    fileDb.getChildren(uuid).flatMap(files => files.traverseM {
      case (id, DatabaseFile(_, _, _, FileType.Directory, _, _)) => getINIFilePaths(id)
      case (id, file @ DatabaseFile(_, _, _, FileType.INI|FileType.Chart, _, _)) => M.pure(IList(makeFileIdentifierPair(id, file)))
      case _ => M.pure(IList())
    })

  def makeFileIdentifierPair(id: UUIDFor[File], file: DatabaseFile): FileIdentifierPair = (id, file.apiKey)

  def readINIFiles[FF[_]: Traverse](entries: FF[FileIdentifierPair]): N[FF[(UUIDFor[File], Maybe[ParseResult])]] =
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


  def handleParsedResults[FF[_]: Traverse](results: FF[(UUIDFor[File], Maybe[ParseResult])]): FF[ValidationNel[SongIndexerError, (UUIDFor[File], INIFile)]] =
    results.map {
      case (id, Empty()) => FileDoesNotExistInFilesystem(id).widen.failureNel
      case (id, Just(Success(file))) => (id, file).successNel
      case (id, Just(Failure(errs))) => errs.map(FailedToParseSongINI(id, _).widen).failure
    }

  def saveSongs[FF[_]: Traverse](data: FF[(UUIDFor[File], INIFile)]): M[FF[ValidationNel[SongIndexerError, UUIDFor[Song]]]] =
    data.traverse((updateOrInsertSongFromINI _).tupled andThen (_.map(_.innerLeftMap(_.iniFileReaderError))))

  def updateOrInsertSongFromINI(uuid: UUIDFor[File], ini: INIFile): M[ValidationNel[CannotSaveSongError, UUIDFor[Song]]] =
    songFromINIData(uuid, ini).traverse(song => updateSong(song).flatMap(_.getOrElseF[M](insertSong(song))))

  def updateSong(song: DatabaseSong): M[Maybe[UUIDFor[Song]]] =
    songDb.updateSongByData(song).map(_.map(_._1))

  def insertSong(song: DatabaseSong): M[UUIDFor[Song]] = {
    val id = UUID.randomUUID().asEntityId[Song]
    songDb.insertSong(id, song).map(_ => id)
  }

  def songFromINIData(uuid: UUIDFor[File], ini: INIFile): ValidationNel[CannotSaveSongError, DatabaseSong] = {
    val songSections = IList(INISectionName("song").just, INISectionName("Song").just)
    val songNameKeys = IList(INIKey("Name"), INIKey("name"))
    val artistKeys   = IList(INIKey("Artist"), INIKey("artist"))
    val albumKeys    = IList(INIKey("Album"), INIKey("album"))
    val genreKeys    = IList(INIKey("genre"), INIKey("Genre"))
    val charterKeys  = IList(INIKey("Charter"), INIKey("frets"))
    val yearKeys     = IList(INIKey("Year"), INIKey("year"))
    val now = Instant.now()

    ini.lookupOneOf(songSections).cata(
      iniSection => { val section = iniSection.map(_.value)
        (uuid.successNel[CannotSaveSongError] |@|
        section.lookupOneOf(songNameKeys).map(SongName(_)).toSuccess(MissingKey(songSections, songNameKeys)).toValidationNel |@|
        section.lookupOneOf(artistKeys).map(Artist(_)).successNel |@|
        section.lookupOneOf(albumKeys).map(Album(_)).successNel |@|
        section.lookupOneOf(genreKeys).map(Genre(_)).successNel |@|
        section.lookupOneOf(charterKeys).map(Charter(_)).successNel |@|
        section.lookupOneOf(yearKeys).map(_.filter(Set(('0' to '9'): _*))) .map(Year(_)).successNel |@|
        now.successNel |@|
        now.successNel)(DatabaseSong.apply _) },
      MissingSection(songSections).widen.failureNel
    )
  }
}

