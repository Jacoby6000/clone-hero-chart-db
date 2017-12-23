package com.jacoby6000.cloneherodb


package object cloneherodb
  extends JavaCompatCancer
    with ScalaPrimitives
    with ScalazTypeclassInstances
    with Syntax
    with CatsCompat {

  type StringContext = scala.StringContext
  val StringContext: scala.StringContext.type = scala.StringContext

  def implicitly[A](implicit a: A): A = a
  def identity[A](a: A): A = a
}





