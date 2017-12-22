package com.jacoby6000.cloneherodb.http.json.data

import java.util.UUID

import argonaut._
import Argonaut._
import com.jacoby6000.cloneherodb.data._
import com.jacoby6000.cloneherodb.syntax.Shows

import scalaz._
import Scalaz._

import scala.collection.immutable.List

object codecs extends DataCodecInstances {


}

trait DataCodecInstances {
  val stringCodec = CodecJson.derived[String]

  implicit val apiKeyTypeCodecJson: CodecJson[ApiKeyType] = {
    stringCodec.xmap(ApiKeyType.withName)(_.entryName)
  }

  implicit def entityIdCodecJson[A, E](implicit idCodec: CodecJson[A]): CodecJson[EntityId[E, A]] =
    idCodec.xmap(EntityId[E, A])(_.value)

  implicit def entityIdEncodeJson[A, E](implicit encodeA: EncodeJson[A]): EncodeJson[EntityId[E, A]] =
    encodeA.contramap[EntityId[E, A]](_.value)

  implicit def entityIdDecodeJson[A, E](implicit decodeA: DecodeJson[A]): DecodeJson[EntityId[E, A]] =
    decodeA.map[EntityId[E, A]](EntityId(_))


  implicit val apiKeyCodecJson: CodecJson[ApiKey] =
    CodecJson.codec2[ApiKeyType, String, ApiKey](
      ApiKey.fromTypeAndKey,
      _.fold(ApiKeyType.GoogleApiKey -> _, ApiKeyType.LocalFSApiKey -> _)
    )("keyType", "key")

  implicit val uuidEncodeJson: EncodeJson[UUID] = stringCodec.contramap[UUID](_.toString)
  implicit val uuidDecodeJson: DecodeJson[UUID] = stringCodec.map(UUID.fromString)

  implicit def ilistEncodeJson[A: EncodeJson]: EncodeJson[IList[A]] = EncodeJson.of[List[A]].contramap[IList[A]](_.toList)
  implicit def ilistDecodeJson[A: DecodeJson]: DecodeJson[IList[A]] = DecodeJson.of[List[A]].map(_.toIList)

  implicit val encodeShows: EncodeJson[Shows] = EncodeJson.jencode1[Shows, String](_.toString)

}