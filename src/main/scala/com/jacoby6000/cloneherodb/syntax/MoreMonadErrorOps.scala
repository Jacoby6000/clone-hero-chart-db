package com.jacoby6000.cloneherodb.syntax

import scalaz.Liskov.<~<
import scalaz.Scalaz._
import scalaz._

class MoreMonadErrorOps[F[_], A](val fa: F[A]) extends AnyVal { self =>
  def liftError[C, D, E](raiseErr: C => F[E])(implicit F: MonadError[F, E], ev: A <~< Validation[C, D]): F[D] =
    for {
      a <- fa
      validation = ev(a)

      result <- validation.fold(raiseErr(_).flatMap(F.raiseError[D]), F.pure(_))
    } yield result

  def liftEmpty[E](implicit FE: MonadError[F, E]): PartiallyAppliedLiftEmpty[F, A, E] =
    new PartiallyAppliedLiftEmpty[F, A, E] {
      override val fa: F[A] = self.fa
      override val F: MonadError[F, E] = FE
    }


}

trait PartiallyAppliedLiftEmpty[F[_], A, E] {
  val fa: F[A]
  implicit val F: MonadError[F, E]
  def apply[B, X](raiseErr: => F[X])(implicit ev: A <~< Maybe[B], ev2: X <~< E): F[B] =
    for {
      a <- fa
      maybe = ev(a)

      result <- maybe.getOrElseF(raiseErr.flatMap(err => F.raiseError[B](ev2(err))))
    } yield result
}
