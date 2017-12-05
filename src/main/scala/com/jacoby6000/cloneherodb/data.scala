package com.jacoby6000.cloneherodb

import java.util.UUID

object data {
  case class SongName(value: String) extends AnyVal
  case class DirectoryName(value: String) extends AnyVal


  sealed trait ApiKey
  case class GoogleApiKey(key: String) extends ApiKey

  case class EntityId[A, B](value: B) extends AnyVal
  type UUIDFor[A] = EntityId[A, UUID]
  type ApiKeyFor[A] = EntityId[A, ApiKey]


  implicit class AllOps[A](val a: A) extends AnyVal {
    def asEntityId[B]: EntityId[B, A] = EntityId(a)
  }
}
