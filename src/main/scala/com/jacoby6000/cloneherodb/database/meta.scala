package com.jacoby6000.cloneherodb.database

import com.jacoby6000.cloneherodb.data._
import doobie._

import scala.reflect.runtime.universe.TypeTag
import scalaz._
import shapeless._

object meta {
  implicit def maybeComposite[A](implicit ev: Meta[A]): Composite[Maybe[A]] =
    Composite.fromMetaOption(ev).imap(Maybe.fromOption)(_.toOption)

  /** This violates the design philosophy. Assuming this doesn't break anything, I'm going to file
    * an issue to doobie so that we can make Param instances without cheating.
    */
  implicit def maybeParam[A](implicit ev: Meta[A]): Param[Maybe[A]] = {
    new Param[Maybe[A]](maybeComposite(ev))
  }

  implicit def valueClassImplicit[A <: AnyVal, L <: HList, S](
    implicit ev: Generic.Aux[A, L],
    toL: (S :: HNil) =:= L,
    toS: L =:= (S :: HNil),
    tt: TypeTag[A],
    meta: Meta[S]
  ): Meta[A] =
    Meta[S].xmap[A](s => ev.from(toL(s :: HNil)), a => toS(ev.to(a)).head)

  implicit val fileTypeMeta: Meta[FileType] =
    Meta[String].xmap[FileType](
      s => FileType.withNameOption(s).getOrElse(FileType.Unknown(s)),
      {
        case FileType.Unknown(s) => s
        case other => other.entryName
      }
    )


  private def valueClassMeta[A]: PartiallyAppliedValueClassMeta[A] = new PartiallyAppliedValueClassMeta[A]

  // once scalaz deriving comes out we can make this better.
  private class PartiallyAppliedValueClassMeta[A] {
    def apply[L <: HList, S](
      implicit ev: Generic.Aux[A, L],
      toL: (S :: HNil) =:= L,
      toS: L =:= (S :: HNil),
      tt: TypeTag[A],
      meta: Meta[S]
    ): Meta[A] =
      Meta[S].xmap[A](s => ev.from(toL(s :: HNil)), a => toS(ev.to(a)).head)
  }

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
