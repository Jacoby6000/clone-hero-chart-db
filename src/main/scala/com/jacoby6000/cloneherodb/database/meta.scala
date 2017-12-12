package com.jacoby6000.cloneherodb.database

import com.jacoby6000.cloneherodb.data._
import doobie._

import scala.reflect.runtime.universe.TypeTag
import scalaz._, Scalaz._

object meta {

  implicit class QueryOps[A](query: Query0[A]) {
    def maybe: ConnectionIO[Maybe[A]] = query.option.map(Maybe.fromOption)
    def iList: ConnectionIO[IList[A]] = query.list.map(_.toIList)
  }

  implicit def maybeComposite[A](implicit ev: Meta[A]): Composite[Maybe[A]] =
    Composite.fromMetaOption(ev).imap(Maybe.fromOption)(_.toOption)

  /** This violates the design philosophy. Assuming this doesn't break anything, I'm going to file
    * an issue to doobie so that we can make Param instances without cheating.
    */
  implicit def maybeParam[A](implicit ev: Meta[A]): Param[Maybe[A]] = {
    val constructor = classOf[Param[Maybe[A]]].getConstructors.head
    constructor.setAccessible(true)
    constructor.newInstance(maybeComposite(ev)).asInstanceOf[Param[Maybe[A]]]
  }

  implicit val songNameMeta: Meta[SongName] =
    Meta[String].xmap[SongName](SongName(_), _.value)

  implicit def entityIdMeta[A, B: Meta](implicit tt: TypeTag[EntityId[A, B]]): Meta[EntityId[A, B]] =
    Meta[B].xmap[EntityId[A, B]](EntityId(_), _.value)

  implicit val fileTypeMeta: Meta[FileType] =
    Meta[String].xmap[FileType](
      s => FileType.withNameOption(s).getOrElse(FileType.Unknown(s)),
      {
        case FileType.Unknown(s) => s
        case other => other.entryName
      }
    )

  implicit val apiKeyMeta: Meta[ApiKey] = {
    Meta[String].xmap(
      { key =>
        val splitKey = key.split(';')
        val keyType = ApiKeyType.withName(splitKey.head)
        val keyString = splitKey.tail.mkString

        ApiKey.fromTypeAndKey(keyType, keyString)
      },
      {
        case GoogleApiKey(key) => s"${ApiKeyType.GoogleApiKey.enumEntry};$key"
        case LocalFSApiKey(key) => s"${ApiKeyType.LocalFSApiKey.enumEntry};$key"
      }
    )
  }

}
