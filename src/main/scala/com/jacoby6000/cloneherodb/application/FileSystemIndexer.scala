package com.jacoby6000.cloneherodb.application

import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

import com.jacoby6000.cloneherodb.application.FileSystemIndexer._
import com.jacoby6000.cloneherodb.filesystem.FileSystem
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.database.DatabaseFiles
import com.jacoby6000.cloneherodb.database.DatabaseFiles.{File => DatabaseFile}
import com.jacoby6000.cloneherodb.logging.Logger

import scalaz.Maybe.{Empty, Just}
import scalaz.Scalaz._
import scalaz._

object FileSystemIndexer {
  sealed trait FileIndexerError
  case class IndexTargetNotFoundInDatabase(id: UUIDFor[File]) extends FileIndexerError
  case class IndexTargetNotFoundInFileSystem(key: ApiKeyFor[File]) extends FileIndexerError

  object FileTree {
    implicit val fileTreeShow: Show[FileTree] =
      Show.show { tree =>
        def go(n: Int, tree: FileTree): Cord =
          Cord.stringToCord(" " * n) ++ (tree match {
            case Leaf(file) => Cord.stringToCord(file.name.value) + "\n"
            case Node(file, children) =>
              Cord.stringToCord(file.name.value) ++
                Cord.stringToCord("\n") ++
                children.foldMap(go(n + 1, _))
          })

        Cord.stringToCord("\n") ++ go(0, tree)
      }
  }
  sealed trait FileTree {
    def findByPath(path: FilePath): Maybe[FileTree] =
      this match {
        case Leaf(file) => if (file.path === path) Just(this) else Empty()
        case Node(file, _) if file.path === path => Just(this)
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

}

trait FileSystemIndexer[F[_]] {
  def newIndex(apiKey: ApiKeyFor[File]): F[UUIDFor[File]]
  def index(id: UUIDFor[File]): F[IList[DatabaseFile]]
}

class FileSystemIndexerImpl[F[_], M[_], N[_]](
  fileDb: DatabaseFiles[M],
  fileSystemProvider: ApiKey => FileSystem[N],
  logger: Logger[F] )(
  mToF: M ~> F,
  nToF: N ~> F)(implicit
  F: MonadError[F, FileIndexerError],
  N: Monad[N],
  M: Monad[M]
) extends FileSystemIndexer[F] {

  def newIndex(apiKey: ApiKeyFor[File]): F[UUIDFor[File]] = {
    val keyPath = apiKeyToPath(apiKey.value)
    val fileSystem = fileSystemProvider(apiKey.value)
    for {
      _ <- logger.verbose(show"Checking if the new index root at $keyPath exists.")

      newIndexFile <- nToF(fileSystem.fileAt(keyPath)).liftEmpty[FileIndexerError].apply {
        logger.info(show"Failed to find index target $apiKey in fs.") *> IndexTargetNotFoundInFileSystem(apiKey).pure[F]
      }

      newIndexId = UUID.randomUUID().asEntityId[File]
      _ <-
        logger.verbose(show"New index root at $keyPath exists. Storing in db with id $newIndexId") *>
        mToF(fileDb.insertFile(newIndexId, fileToDatabaseFile(newIndexFile, Empty(), makePathToApiKeyFunc(apiKey.value)))) <*
        logger.verbose(show"Successfully stored new index root $newIndexId")
    } yield newIndexId
  }

  def index(id: UUIDFor[File]): F[IList[DatabaseFile]] = {
    for {
      dbFile <- mToF(fileDb.getFile(id)).liftEmpty[FileIndexerError].apply {
        logger.info(show"Failed to find index target $id in db.") *> IndexTargetNotFoundInDatabase(id).pure[F]
      }
      apiKey = dbFile.apiKey

      fileSystem = fileSystemProvider(apiKey.value)
      fileSystemTree <- nToF(fileTree(apiKeyToPath(apiKey.value), Empty(), fileSystem)).liftEmpty[FileIndexerError].apply {
        logger.info(show"Failed to find index target $apiKey") *> IndexTargetNotFoundInFileSystem(apiKey).pure[F]
      }
      storedTree <- mToF(storeTree(fileSystemTree, Empty(), makePathToApiKeyFunc(dbFile.apiKey.value)))
    } yield storedTree
  }

  def apiKeyToPath(key: ApiKey): FilePath =
    key.fold(k => filePath(PathPart(k)), k => filePath(Paths.get(k)))

  def fileTree(start: FilePath, maxDepth: Maybe[Int], fileSystem: FileSystem[N]): N[Maybe[FileTree]] =
    fileSystem.fileAt(start).flatMap(_.cata(
      file => file.fileType match {
        case FileType.Directory =>
          for {
            subFiles <- fileSystem.childrenOf(file.path)
            trees <- subFiles.traverse(subFile => fileTree(subFile.path, maxDepth.map(_ - 1), fileSystem))
          } yield {
            Just(Node(file, trees.flatMap(_.toIList)))
          }
        case _ =>
          N.point(Just(Leaf(file)))

      },
      N.point(Empty())
    ))

  def makePathToApiKeyFunc(initialKey: ApiKey): FilePath => ApiKey =
    initialKey.fold(
      _ => filePath => GoogleApiKey(filePath.end.value),
      _ => filePath => LocalFSApiKey(filePath.javaPath.toString)
    )


  def storeTree(tree: FileTree, parent: Maybe[UUIDFor[File]], pathToApiKey: FilePath => ApiKey): M[IList[DatabaseFile]] = {
    val fileId: UUIDFor[File] = UUID.randomUUID().asEntityId

    def saveDatabaseFile(file: DatabaseFile): M[(UUIDFor[File], DatabaseFile)] =
      fileDb.updateFileByApiKey(file).flatMap (
        _.cata(
          M.point(_),
          fileDb.insertFile(fileId, file).map(_ => fileId -> file)
        )
      )

    def saveFile(file: File): M[(UUIDFor[File], DatabaseFile)] =
      saveDatabaseFile(fileToDatabaseFile(file, parent, pathToApiKey))

    tree match {
      case Node(file, subTree) =>
        saveFile(file).flatMap { case (id, savedFile) =>
          subTree.traverseM(storeTree(_, id.just, pathToApiKey)).map(savedFile :: _)
        }

      case Leaf(file) => saveFile(file).map(_._2).map(IList(_))
    }
  }

  def fileToDatabaseFile(file: File, parent: Maybe[UUIDFor[File]], pathToApiKey: FilePath => ApiKey): DatabaseFile =
    DatabaseFile(
      FileName(file.name.value),
      pathToApiKey(file.path).asEntityId,
      parent,
      file.fileType,
      Instant.now(),
      Instant.now()
    )
}
