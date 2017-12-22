package com.jacoby6000.cloneherodb.http.services

import argonaut._
import cats.effect.Effect
import com.jacoby6000.cloneherodb.application.SongIndexer._
import com.jacoby6000.cloneherodb.application.{FileSystemIndexer, SongIndexer}
import com.jacoby6000.cloneherodb.data.{ApiKeyFor, File}
import com.jacoby6000.cloneherodb.http.json.data.codecs._
import com.jacoby6000.cloneherodb.http.services.IndexerService._
import com.jacoby6000.cloneherodb.logging.Logger
import com.jacoby6000.cloneherodb.syntax._
import com.jacoby6000.cloneherodb.data._

import scalaz._
import Scalaz._
import shims._

object IndexerService {
  case class NewFilesystemRoot(apiKey: ApiKeyFor[File])
  object NewFilesystemRoot {
    implicit val newFilesystemRootDecodeJson: DecodeJson[NewFilesystemRoot] =
      DecodeJson.jdecode1L(NewFilesystemRoot.apply)("apiKey")
  }

  case class ResourceIdResponse[A](id: A)
  object ResourceIdResponse {
    implicit def resourceIdResponseEncodeJson[A](implicit encoder: EncodeJson[A]): EncodeJson[ResourceIdResponse[A]] =
      EncodeJson.jencode1L[ResourceIdResponse[A], A](_.id)("id")
  }

  implicit val songIndexerErrorCodec: EncodeJson[SongIndexerError] = EncodeJson.jencode1[SongIndexerError, Shows]({
    case FileRootNotFoundInDatabase(uuid) => show"Root does not exist in db: $uuid"
    case FileRootNotFoundInFilesystem(key) => show"Root found in db but not on filesystem: $key"
    case FailedToParseSongINI(uuid, error) => show"Failed to parse song configuration file for song $uuid.  Reason: ${error.toString}"
    case FileDoesNotExistInFilesystem(uuid) => show"File from db does not exist in filesystem: $uuid"
    case FailedToSaveSong(err) => show"Failed to save song: ${err.toString}"
  })

  case class ResultWithFailures[+A, +B](failures: A, result: B)
  object ResultWithFailures {
    implicit def resultWithFailuresEncodeJson[A: EncodeJson, B: EncodeJson]: EncodeJson[ResultWithFailures[A, B]] =
      EncodeJson.jencode2L[ResultWithFailures[A, B], A, B](ResultWithFailures.unapply(_).get)("failures", "result")

    implicit def resultWithFailuresMonoid[A: Monoid, B: Monoid]: Monoid[ResultWithFailures[A, B]] =
      Monoid.instance[ResultWithFailures[A, B]](
        (l, r) => ResultWithFailures(l.failures |+| r.failures, l.result |+| r.result),
        ResultWithFailures(Monoid[A].zero, Monoid[B].zero)
      )
  }

}

class IndexerService[F[_] : Effect, N[_], M[_]](
  fileIndexer: FileSystemIndexer[N],
  songIndexer: SongIndexer[M],
  nToF: N ~> F,
  mToF: M ~> F,
  logger: Logger[F]
) extends Http4sService[F] {

  val IndexerRoot = Root / "index" / "roots"

  val service: Service = Service {
    case PUT -> IndexerRoot / UUIDForFileVar(uuid) =>
      logger.verbose("Recieved re-index root request.")
      nToF(fileIndexer.index(uuid)).flatMap(files => Ok(files.map(_.apiKey.value)))

    case PUT -> IndexerRoot / UUIDForFileVar(uuid) / "songs" =>
      logger.verbose("Recieved re-index root request.")
      mToF(songIndexer.indexSongsAtRoot(uuid))
        .flatMap { files =>
          val gatheredResults =
            files.foldMap[ResultWithFailures[IList[SongIndexerError], IList[UUIDFor[Song]]]](_.fold(
              nel => ResultWithFailures(nel.toIList, IList.empty),
              id => ResultWithFailures(IList.empty, IList(id))
            ))
          Ok(gatheredResults)
        }

    case req @ POST -> IndexerRoot =>
      logger.verbose("Recieved new index root request.")
      req.decode[NewFilesystemRoot] { root =>
        logger.verbose("Successfully decoded new filesystem root: " + root.apiKey.value.show)
        nToF(fileIndexer.newIndex(root.apiKey)).map(ResourceIdResponse(_)).flatMap(Ok(_))
      }
  }
}
