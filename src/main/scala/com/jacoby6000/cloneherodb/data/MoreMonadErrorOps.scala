package com.jacoby6000.cloneherodb.data


import scalaz._
import Scalaz._
import scalaz.Liskov.<~<

class MoreMonadErrorOps[F[_], A](val fa: F[A]) extends AnyVal {
  def liftError[G[_], C, D, E](nt: F ~> G)(raiseErr: C => G[E])(implicit G: MonadError[G, E], ev: A <~< Validation[C, D]): G[D] =
    for {
      a <- nt(fa)
      validation = ev(a)

      result <- validation.fold(raiseErr(_).flatMap(G.raiseError[D]), G.pure(_))
    } yield result

  def liftEmpty[G[_], C, E](nt: F ~> G)(raiseErr: => G[E])(implicit G: MonadError[G, E], ev: A <~< Maybe[C]): G[C] =
    for {
      a <- nt(fa)
      maybe = ev(a)

      result <- maybe.getOrElseF(raiseErr.flatMap(G.raiseError[C]))
    } yield result
}
