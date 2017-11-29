package com.jacoby6000.cloneherodb

import scalaz._
import java.util.UUID

object data {
  case class SongName(value: String) extends AnyVal
  case class DirectoryName(value: String) extends AnyVal


  sealed trait ApiKey
  case class GoogleApiKey(key: String) extends AnyVal

  case class EntityId[A, B](value: B) extends AnyVal
  type UUIDFor[A] = EntityId[A, UUID]
  type GoogleIdFor[A] = EntityId[A, GoogleApiKey]


  case class WithId[A, B](id: EntityId[B, A], record: B)
  object WithId {
    implicit def withIdFunctor[I]: InvariantFunctor[WithId[I, ?]] =
      new Functor[WithId[I, ?]] {
        def map[A, B](withId: WithId[I, A])(f: A => B): WithId[I, B] =
          WithId(EntityId(withId.id.value), f(withId.record))
      }
  }

  implicit class AllOps[A](val a: A) extends AnyVal {
    def asEntityId[B]: EntityId[B, A] = EntityId(a)
    def withId[B](id: EntityId[A, B]): WithId[B, A] = WithId(id, a)
  }
}
