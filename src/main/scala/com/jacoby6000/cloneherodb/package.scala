package com.jacoby6000

import cats.effect.Async
import shims._

import scalaz._, Scalaz._

package object cloneherodb {
  implicit val dequeueMonad: MonadPlus[Dequeue] =
    new MonadPlus[Dequeue] {
      override def point[A](a: => A): Dequeue[A] = Dequeue(a)
      override def bind[A, B](fa: Dequeue[A])(f: (A) => Dequeue[B]) = fa.foldMap(f)
      override def empty[A] = Dequeue.empty[A]
      override def plus[A](a: Dequeue[A], b: => Dequeue[A]) = a ++ b
    }

  implicit def eitherTAsync[M[_]: Async, E]: Async[EitherT[M, E, ?]] = ScopeSeparator.eitherTAsyncInstance
}

private object ScopeSeparator {
  def eitherTAsyncInstance[M[_], E](implicit M: Async[M]): Async[EitherT[M, E, ?]] =
    new Async[EitherT[M, E, ?]] {
      override def async[A](k: (Either[Throwable, A] => Unit) => Unit): EitherT[M, E, A] =
        EitherT.right(M.async(k))

      override def suspend[A](thunk: => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.suspend(M.pure(thunk))).flatMap(identity)

      override def flatMap[A, B](fa: EitherT[M, E, A])(f: A => EitherT[M, E, B]): EitherT[M, E, B] =
        fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A => EitherT[M, E, Either[A, B]]): EitherT[M, E, B] =
        BindRec[EitherT[M, E, ?]].tailrecM(f andThen (_.map(_.disjunction)))(a)

      override def raiseError[A](e: Throwable): EitherT[M, E, A] =
        EitherT.right(M.raiseError(e))

      override def handleErrorWith[A](fa: EitherT[M, E, A])(f: Throwable => EitherT[M, E, A]): EitherT[M, E, A] =
        EitherT.right(M.handleErrorWith(M.pure(fa))(f andThen M.pure)).flatMap(identity)

      override def pure[A](x: A): EitherT[M, E, A] = EitherT.right(M.pure(x))
    }
}
