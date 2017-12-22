package com.jacoby6000.cloneherodb.syntax

import com.jacoby6000.cloneherodb.data.EntityId

import scalaz._
import Scalaz._
import scalaz.Maybe.Empty

class AllOps[A](val a: A) extends AnyVal {
  def asEntityId[B]: EntityId[B, A] = EntityId(a)

  def justIf(f: A => Boolean): Maybe[A] = if (f(a)) a.just else Empty()
}


