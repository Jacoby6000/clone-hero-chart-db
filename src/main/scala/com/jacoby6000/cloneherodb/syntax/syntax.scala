package com.jacoby6000.cloneherodb

import com.jacoby6000.cloneherodb.data.FilePath

import scalaz._

package object syntax {
  implicit def toFilePathOps(filePath: FilePath): FilePathOps =
    new FilePathOps(filePath)

  implicit def toStringContextCloneHeroOps(filePath: StringContext): StringContextCloneHeroOps =
    new StringContextCloneHeroOps(filePath)

  implicit def toMonadErrorOps[F[_], A](fa: F[A]): MoreMonadErrorOps[F, A] = new MoreMonadErrorOps(fa)

  implicit def allOps[A](a: A): AllOps[A] =
    new AllOps(a)

  implicit def toIMapOps[A, B](imap: IMap[A, B]): IMapOps[A, B] = new IMapOps(imap)

  implicit def toBifunctorCloneHeroOps[F[_, _], A, B](fab: F[A, B]): BifunctorCloneHeroOps[F, A, B] =
    new BifunctorCloneHeroOps(fab)

  implicit def toBifunctorCloneHeroLeftMOps[F[_, _], G[_], A, B](fab: F[G[A], B]): BifunctorCloneHeroLeftMOps[F, G, A, B] =
    new BifunctorCloneHeroLeftMOps(fab)

  implicit def toBifunctorCloneHeroRightMOps[F[_, _], G[_], A, B](fab: F[A, G[B]]): BifunctorCloneHeroRightMOps[F, G, A, B] =
    new BifunctorCloneHeroRightMOps(fab)
}

