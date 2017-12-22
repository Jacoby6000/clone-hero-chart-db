package com.jacoby6000.cloneherodb.syntax

import scalaz._
import Scalaz._

class BifunctorCloneHeroOps[F[_, _], A, B](val fab: F[A, B]) extends AnyVal {

}

class BifunctorCloneHeroLeftMOps[F[_, _], G[_], A, B](val fgab: F[G[A], B]) extends AnyVal {
  def innerLeftMap[C](f: A => C)(implicit F: Bifunctor[F], G: Functor[G]): F[G[C], B] =
    fgab.leftMap(_.map(f))
}

class BifunctorCloneHeroRightMOps[F[_, _], G[_], A, B](val fagb: F[A, G[B]]) extends AnyVal {
  def innerRightMap[C](f: B => C)(implicit F: Bifunctor[F], G: Functor[G]): F[A, G[C]] =
    fagb.rightMap(_.map(f))
}
