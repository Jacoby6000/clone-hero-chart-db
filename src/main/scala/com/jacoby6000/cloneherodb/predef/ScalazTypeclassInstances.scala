package com.jacoby6000.cloneherodb.predef

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
