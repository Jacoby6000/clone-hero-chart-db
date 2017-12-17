package com.jacoby6000.cloneherodb

import com.jacoby6000.cloneherodb.data.FilePath

import scalaz._, Scalaz._

package object syntax {
  implicit def toFilePathOps(filePath: FilePath): FilePathOps =
    new FilePathOps(filePath)

  implicit def toFilePathStringContextOps(filePath: StringContext): FilePathStringContextOps =
    new FilePathStringContextOps(filePath)

  implicit def toMonadErrorOps[F[_], A](fa: F[A]): MoreMonadErrorOps[F, A] = new MoreMonadErrorOps(fa)

  implicit def allOps[A](a: A): AllOps[A] =
    new AllOps(a)

  implicit def showsStringContext(sc: StringContext): ShowInterpolator = new ShowInterpolator(sc)

  implicit def toIMapOps[A, B](imap: IMap[A, B]): IMapOps[A, B] = new IMapOps(imap)

  implicit val dequeueMonad: MonadPlus[Dequeue] =
    new MonadPlus[Dequeue] {
      override def point[A](a: => A): Dequeue[A] = Dequeue(a)
      override def bind[A, B](fa: Dequeue[A])(f: (A) => Dequeue[B]) = fa.foldMap(f)
      override def empty[A] = Dequeue.empty[A]
      override def plus[A](a: Dequeue[A], b: => Dequeue[A]) = a ++ b
    }

}
