package com.jacoby6000.cloneherodb.database

import doobie.free.connection.ConnectionIO
import doobie.util.query.Query0
import doobie.util.update.Update0

import scalaz._, Scalaz._

object syntax {
  implicit class QueryOps[A](query: Query0[A]) {
    def maybe: ConnectionIO[Maybe[A]] = query.option.map(Maybe.fromOption)
    def iList: ConnectionIO[IList[A]] = query.list.map(_.toIList)
  }

  implicit class UpdateOps[A](query: Update0) {
    def runUnit: ConnectionIO[Unit] = query.run.map(_ => ())
  }

}
