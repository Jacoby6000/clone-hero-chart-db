package com.jacoby6000.cloneherodb.syntax

import com.jacoby6000.cloneherodb.data.PathPart

class StringContextCloneHeroOps(val sc: StringContext) extends AnyVal {
  def path(args: String*): PathPart = PathPart(sc.s(args: _*))
  def show(args: Shows*): Shows = Shows(sc.s(args: _*))
}
