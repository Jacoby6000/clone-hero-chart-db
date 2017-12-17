package com.jacoby6000.cloneherodb.syntax

import scalaz.Show

/*
 * https://github.com/scalaz/scalaz/pull/1496
 */

final case class Shows private (override val toString: String) extends AnyVal
object Shows {
  implicit def mat[A](x: A)(implicit S: Show[A]): Shows = Shows(S.shows(x))
}

final class ShowInterpolator(val sc: StringContext) extends AnyVal {
  def show(args: Shows*): Shows = Shows(sc.s(args: _*))
}
