package com.jacoby6000.cloneherodb.http.json.data

import java.util.UUID

import argonaut._
import Argonaut._
import com.jacoby6000.cloneherodb.data._

object codecs extends DataCodecInstances {


}

trait DataCodecInstances {
  val stringCodec = CodecJson.derived[String]

  implicit val apiKeyTypeCodecJson: CodecJson[ApiKeyType] = {
    stringCodec.xmap(ApiKeyType.withName)(_.entryName)
  }

  implicit def entityIdCodecJson[A, E](implicit idCodec: CodecJson[A]): CodecJson[EntityId[E, A]] =
    idCodec.xmap(EntityId[E, A])(_.value)

  implicit val apiKeyCodecJson: CodecJson[ApiKey] =
    CodecJson.codec2[ApiKeyType, String, ApiKey](
      ApiKey.fromTypeAndKey,
      _.fold(ApiKeyType.GoogleApiKey -> _, ApiKeyType.LocalFSApiKey -> _)
    )("keyType", "key")

  implicit val uuidCodec: CodecJson[UUID] = stringCodec.xmap(UUID.fromString _)(_.toString)
}