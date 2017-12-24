package com.jacoby6000.cloneherodb

import scalaz._
import Scalaz._
import cats.effect.Async
import java.lang.Throwable
import scala.Either
import shims._

trait CatsCompat extends ScalaPrimitives {


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
