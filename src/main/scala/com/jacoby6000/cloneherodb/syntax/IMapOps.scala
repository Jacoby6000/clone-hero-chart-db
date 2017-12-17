package com.jacoby6000.cloneherodb.syntax

import scalaz._, Scalaz._

class IMapOps[A, B](val imap: IMap[A, B]) extends AnyVal {
  def lookupOneOf(keys: IList[A])(implicit A: Order[A]): Maybe[B] = {
    def go(result: Option[B], goKeys: IList[A]): Option[B] =
      result match {
        case Some(a) => Some(a)
        case None =>
          goKeys match {
            case INil() => None
            case ICons(head, tail) => go(imap.lookup(head), tail)
          }
      }

    go(None, keys).toMaybe
  }
}
