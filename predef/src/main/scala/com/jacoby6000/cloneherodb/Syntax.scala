package com.jacoby6000.cloneherodb.predef

import com.jacoby6000.LowPrioritySyntax

import scala.collection.mutable.ArrayOps

trait Syntax extends LowPrioritySyntax {

  implicit def arrowAssocOps[A](a: A): scala.Predef.ArrowAssoc[A] = new scala.Predef.ArrowAssoc[A](a)

  import scala.collection.immutable.StringOps
  import scala.collection.mutable

  @inline implicit def stringOps(s: String): StringOps = new StringOps(s)
  @inline implicit def unstringOps(s: StringOps): String = s.repr

  implicit def genericArrayOps[T](xs: Array[T]): ArrayOps[T] = (xs match {
    case x: Array[AnyRef]  => refArrayOps[AnyRef](x)
    case x: Array[Boolean] => booleanArrayOps(x)
    case x: Array[Byte]    => byteArrayOps(x)
    case x: Array[Char]    => charArrayOps(x)
    case x: Array[Double]  => doubleArrayOps(x)
    case x: Array[Float]   => floatArrayOps(x)
    case x: Array[Int]     => intArrayOps(x)
    case x: Array[Long]    => longArrayOps(x)
    case x: Array[Short]   => shortArrayOps(x)
    case x: Array[Unit]    => unitArrayOps(x)
    case _              => null
  }).asInstanceOf[ArrayOps[T]]

  @inline implicit def booleanArrayOps(xs: Array[Boolean]): mutable.ArrayOps.ofBoolean   = new mutable.ArrayOps.ofBoolean(xs)
  @inline implicit def byteArrayOps(xs: Array[Byte]): mutable.ArrayOps.ofByte            = new mutable.ArrayOps.ofByte(xs)
  @inline implicit def charArrayOps(xs: Array[Char]): mutable.ArrayOps.ofChar            = new mutable.ArrayOps.ofChar(xs)
  @inline implicit def doubleArrayOps(xs: Array[Double]): mutable.ArrayOps.ofDouble      = new mutable.ArrayOps.ofDouble(xs)
  @inline implicit def floatArrayOps(xs: Array[Float]): mutable.ArrayOps.ofFloat         = new mutable.ArrayOps.ofFloat(xs)
  @inline implicit def intArrayOps(xs: Array[Int]): mutable.ArrayOps.ofInt               = new mutable.ArrayOps.ofInt(xs)
  @inline implicit def longArrayOps(xs: Array[Long]): mutable.ArrayOps.ofLong            = new mutable.ArrayOps.ofLong(xs)
  @inline implicit def refArrayOps[T <: AnyRef](xs: Array[T]): mutable.ArrayOps.ofRef[T] = new mutable.ArrayOps.ofRef[T](xs)
  @inline implicit def shortArrayOps(xs: Array[Short]): mutable.ArrayOps.ofShort         = new mutable.ArrayOps.ofShort(xs)
  @inline implicit def unitArrayOps(xs: Array[Unit]): mutable.ArrayOps.ofUnit            = new mutable.ArrayOps.ofUnit(xs)

  implicit val stringCanBuildFrom: scala.collection.generic.CanBuildFrom[String, Char, String] =
    scala.Predef.StringCanBuildFrom
}

trait LowPrioritySyntax extends ScalaPrimitives {
  import scala.runtime
  import scala.inline

  @inline implicit def byteWrapper(x: Byte): RichByte          = new runtime.RichByte(x)
  @inline implicit def shortWrapper(x: Short): RichShort       = new runtime.RichShort(x)
  @inline implicit def intWrapper(x: Int): RichInt             = new runtime.RichInt(x)
  @inline implicit def charWrapper(c: Char): RichChar          = new runtime.RichChar(c)
  @inline implicit def longWrapper(x: Long): RichLong          = new runtime.RichLong(x)
  @inline implicit def floatWrapper(x: Float): RichFloat       = new runtime.RichFloat(x)
  @inline implicit def doubleWrapper(x: Double): RichDouble    = new runtime.RichDouble(x)
  @inline implicit def booleanWrapper(x: Boolean): RichBoolean = new runtime.RichBoolean(x)
}

