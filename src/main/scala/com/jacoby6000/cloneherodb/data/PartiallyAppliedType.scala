package com.jacoby6000.cloneherodb.data

trait PartiallyAppliedType[A] {
  type C
  def apply[B](b: B): C
}

