package com.jacoby6000.cloneherodb.application

import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.database.DatabaseSongs
import com.jacoby6000.cloneherodb.database.Songs.{File => DatabaseFile}
import java.time.Instant
import java.util.UUID

import scalaz._
import Scalaz._
import Indexer._
import com.jacoby6000.cloneherodb.application.filesystem.FileSystem

import scalaz.Maybe.{Empty, Just}

object Indexer {
  sealed trait IndexerError
  case class IndexTargetNotFoundInDatabase(id: UUIDFor[File]) extends IndexerError
  case class IndexTargetNotFoundInFileSystem(key: ApiKey) extends IndexerError

  sealed trait FileTree {
    def findByPath(path: FilePath): Maybe[FileTree] =
      this match {
        case Leaf(file) => if (file.path == path) Just(this) else Empty()
        case Node(file, _) if file.path == path => Just(this)
        case Node(file, children) if file.path.containsSub(path) =>
          children.flatMap(_.findByPath(path).toIList).headMaybe

      }

    def fold[A](node: (File, IList[A]) => A, leaf: File => A): A =
      this match {
        case Leaf(file) => leaf(file)
        case Node(file, children) => node(file, children.map(_.fold(node, leaf)))
      }

  }
  case class Node(file: File, children: IList[FileTree]) extends FileTree
  case class Leaf(file: File) extends FileTree

  sealed trait StoreTreeError { def widen: StoreTreeError = this }
  case class NonDirectoryInDirectoryPosition(file: File) extends StoreTreeError
  case class NonDirectoryLeaf(file: File) extends StoreTreeError

}

trait Indexer[F[_]] {
  def index(id: UUIDFor[File]): F[ValidationNel[StoreTreeError, List[DatabaseFile]]]
}

class IndexerImpl[F[_], M[_], N[_]](
  songDb: DatabaseSongs[M],
  fileSystem: FileSystem[N])(
  mToF: M ~> F, nToF: N ~> F
)(implicit
    F: MonadError[F, IndexerError],
    N: Monad[N],
    M: Monad[M]
) extends Indexer[F] {

  def index(id: UUIDFor[File]): F[ValidationNel[StoreTreeError, List[DatabaseFile]]] = {
    for {
      maybeDbFile <- mToF(songDb.getFile(id))
      dbFile <- maybeDbFile.getOrElseF(F.raiseError[DatabaseFile](IndexTargetNotFoundInDatabase(id)))

      maybeFileSystemTree <-nToF(fileTree(apiKeyToPath(dbFile.apiKey.value), Empty()))
      fileSystemTree <- maybeFileSystemTree.getOrElseF(F.raiseError[FileTree](IndexTargetNotFoundInFileSystem(dbFile.apiKey.value)))

      storedTree <- mToF(storeTree(fileSystemTree, Empty()))
    } yield storedTree
  }


  def apiKeyToPath(key: ApiKey): FilePath =
    key match {
      case GoogleApiKey(key) => filePath(PathPart(key))
    }

  def pathToApiKey(path: FilePath): ApiKey =
    GoogleApiKey(path.end.value)

  def fileTree(start: FilePath, maxDepth: Maybe[Int]): N[Maybe[FileTree]] =
    fileSystem.fileAt(start).flatMap {
      case Empty() => N.point(Empty())
      case Just(file) =>
        file.fileType match {
          case FileType.Directory =>
            for {
              subFiles <- fileSystem.childrenOf(file.path)
              trees <- subFiles.traverse(subFile => fileTree(subFile.path, maxDepth.map(_ - 1)))
            } yield {
              Just(Node(file, trees.flatMap(_.toIList)))
            }
          case _ =>
            N.point(Just(Leaf(file)))
        }
    }

  def storeTree(tree: FileTree, parent: Maybe[UUIDFor[File]]): M[ValidationNel[StoreTreeError, List[DatabaseFile]]] = {
    lazy val fileId: UUIDFor[File] = UUID.randomUUID().asEntityId

    def saveFile(file: DatabaseFile): M[Unit] =
      songDb
        .updateFileByApiKey(file)
        .foldM(_.apply(fileId), M.point(_))

    def saveDir(file: File): M[ValidationNel[StoreTreeError, List[DatabaseFile]]] =
      fileToDirectory(file, parent)
        .traverse(dir => saveFile(dir).map(_ => List(dir)))
        .map(_.toValidationNel)

    tree match {
      case Node(file, subTree) =>
        for {
          saveDirResult <- saveDir(file)
          saveSubtreeResult <- subTree.traverse(storeTree(_, Just(fileId)))
        } yield (saveDirResult :: saveSubtreeResult).suml

      case Leaf(file) =>
        file.fileType match {
          case FileType.Directory =>
            saveDir(file)
          case _ =>
            M.point(NonDirectoryLeaf(file).widen.failureNel[List[DatabaseFile]])
        }
    }
  }

  def fileToDirectory(file: File, parent: Maybe[UUIDFor[File]]): Validation[StoreTreeError, DatabaseFile] =
    file.fileType match {
      case FileType.Directory =>
        DatabaseFile(
          FileName(file.name.value),
          pathToApiKey(file.path).asEntityId,
          parent,
          file.fileType,
          Instant.now(),
          Instant.now()
        ).success
      case _ =>
        NonDirectoryInDirectoryPosition(file).failure
    }

}
