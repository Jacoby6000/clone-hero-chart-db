package com.jacoby6000

import cats.effect.Async
import shims._

import scala.collection.mutable.ArrayOps
import scala.runtime._


package object cloneherodb
  extends JavaCompatCancer
    with ScalaPrimitives
    with ScalazTypeclassInstances
    with CloneheroDbPredefSyntax
    with CatsCompat {

  type StringContext = scala.StringContext
  val StringContext: scala.StringContext.type = scala.StringContext

  def implicitly[A](implicit a: A): A = a
  def identity[A](a: A): A = a
}

trait CloneheroDbPredefSyntax extends LowPrioritySyntax {

  implicit def arrowAssocOps[A](a: A): scala.Predef.ArrowAssoc[A] = new scala.Predef.ArrowAssoc[A](a)

  import scala.collection.mutable
  import scala.collection.immutable.StringOps
  import scala.inline

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

// Scalaz typeclass instances which should come from scalaz, but don't.  Anything in here should be PR'd to scalaz.
trait ScalazTypeclassInstances {
  import scalaz._
  import Scalaz._
  implicit val dequeueMonad: MonadPlus[Dequeue] =
    new MonadPlus[Dequeue] {
      override def point[A](a: => A): Dequeue[A] = Dequeue(a)
      override def bind[A, B](fa: Dequeue[A])(f: (A) => Dequeue[B]) = fa.foldMap(f)
      override def empty[A] = Dequeue.empty[A]
      override def plus[A](a: Dequeue[A], b: => Dequeue[A]) = a ++ b
    }
}

trait CatsCompat {
  import scalaz._
  import Scalaz._
  import scala.Either
  import java.lang.Throwable

  implicit def eitherTAsyncInstance[M[_], E](implicit M: Async[M]): Async[EitherT[M, E, ?]] =
    new Async[EitherT[M, E, ?]] {
      override def async[A](k: (Either[Throwable, A] => Unit) => Unit): EitherT[M, E, A] =
        EitherT.right(M.async(k))

      override def suspend[A](thunk: => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.suspend(M.pure(thunk))).flatMap(identity)

      override def flatMap[A, B](fa: EitherT[M, E, A])(f: A => EitherT[M, E, B]): EitherT[M, E, B] =
        fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A => EitherT[M, E, Either[A, B]]): EitherT[M, E, B] =
        EitherT.eitherTBindRec[M, E].tailrecM(f andThen (_.map(_.disjunction)))(a)

      override def raiseError[A](e: Throwable): EitherT[M, E, A] =
        EitherT.right(M.raiseError(e))

      override def handleErrorWith[A](fa: EitherT[M, E, A])(f: Throwable => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.handleErrorWith(M.pure(fa))(f andThen M.pure)).flatMap(identity)

      override def pure[A](x: A): EitherT[M, E, A] = EitherT.right(M.pure(x))
    }

}

trait JavaCompatCancer extends ScalaPrimitives {
  import scala.inline

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