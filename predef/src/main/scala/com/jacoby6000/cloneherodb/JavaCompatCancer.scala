package com.jacoby6000.cloneherodb.predef

trait JavaCompatCancer extends ScalaPrimitives {

  @inline implicit def byte2Byte(x: Byte): java.lang.Byte             = x.asInstanceOf[java.lang.Byte]
  @inline implicit def short2Short(x: Short): java.lang.Short         = x.asInstanceOf[java.lang.Short]
  @inline implicit def char2Character(x: Char): java.lang.Character   = x.asInstanceOf[java.lang.Character]
  @inline implicit def int2Integer(x: Int): java.lang.Integer         = x.asInstanceOf[java.lang.Integer]
  @inline implicit def long2Long(x: Long): java.lang.Long             = x.asInstanceOf[java.lang.Long]
  @inline implicit def float2Float(x: Float): java.lang.Float         = x.asInstanceOf[java.lang.Float]
  @inline implicit def double2Double(x: Double): java.lang.Double     = x.asInstanceOf[java.lang.Double]
  @inline implicit def boolean2Boolean(x: Boolean): java.lang.Boolean = x.asInstanceOf[java.lang.Boolean]

  @inline implicit def Byte2byte(x: java.lang.Byte): Byte             = x.asInstanceOf[Byte]
  @inline implicit def Short2short(x: java.lang.Short): Short         = x.asInstanceOf[Short]
  @inline implicit def Character2char(x: java.lang.Character): Char   = x.asInstanceOf[Char]
  @inline implicit def Integer2int(x: java.lang.Integer): Int         = x.asInstanceOf[Int]
  @inline implicit def Long2long(x: java.lang.Long): Long             = x.asInstanceOf[Long]
  @inline implicit def Float2float(x: java.lang.Float): Float         = x.asInstanceOf[Float]
  @inline implicit def Double2double(x: java.lang.Double): Double     = x.asInstanceOf[Double]
  @inline implicit def Boolean2boolean(x: java.lang.Boolean): Boolean = x.asInstanceOf[Boolean]

}
