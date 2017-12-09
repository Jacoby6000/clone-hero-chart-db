package com.jacoby6000.cloneherodb.database

import doobie._
import com.jacoby6000.cloneherodb.data._
import enumeratum.{Enum, EnumEntry}

import scala.reflect.runtime.universe.TypeTag
import scalaz._

object meta {

  implicit class QueryOps[A](query: Query0[A]) {
    def maybe: ConnectionIO[Maybe[A]] = query.option.map(Maybe.fromOption)
  }

  implicit def maybeComposite[A](implicit composite: Composite[Option[A]]): Composite[Maybe[A]] =
    composite.imap(Maybe.fromOption)(_.toOption)

  implicit val songNameMeta: Meta[SongName] =
    Meta[String].xmap[SongName](SongName(_), _.value)

  implicit def entityIdMeta[A, B: Meta](implicit tt: TypeTag[EntityId[A, B]]): Meta[EntityId[A, B]] =
    Meta[B].xmap[EntityId[A, B]](EntityId(_), _.value)

  implicit val fileTypeMeta: Meta[FileType] =
    Meta[String].xmap[FileType](FileType.withName(_), _.entryName)

  implicit val apiKeyMeta: Meta[ApiKey] = {
    sealed trait ApiKeyType extends EnumEntry
    object ApiKeyType extends Enum[ApiKeyType] {
      case object GoogleApiKey extends ApiKeyType

      val values = findValues
    }

    Meta[String].xmap(
      { key =>
        val splitKey = key.split(';')
        val keyType = ApiKeyType.withName(splitKey.head)
        val keyString = splitKey.tail.mkString

        keyType match {
          case ApiKeyType.GoogleApiKey => GoogleApiKey(keyString)
        }
      },
      {
        case GoogleApiKey(key) => s"${ApiKeyType.GoogleApiKey.enumEntry};$key"
      }
    )
  }

}
