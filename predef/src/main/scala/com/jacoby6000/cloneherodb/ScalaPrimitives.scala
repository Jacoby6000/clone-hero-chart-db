package com.jacoby6000.cloneherodb

trait ScalaPrimitives {
  type AnyRef = scala.AnyRef
  type AnyVal = scala.AnyVal
  type Unit = scala.Unit
  type Int = scala.Int
  type Long = scala.Long
  type Float = scala.Float
  type Double = scala.Double
  type Byte = scala.Byte
  type Short = scala.Short
  type Boolean = scala.Boolean
  type Char = scala.Char
  type String = scala.Predef.String
  type =:=[A, B] = scala.Predef.=:=[A, B]
  type Array[A] = scala.Array[A]
  val Array = scala.Array
}
