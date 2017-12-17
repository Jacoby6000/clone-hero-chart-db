package com.jacoby6000.cloneherodb.logging

import com.jacoby6000.cloneherodb.logging.LogLevel.{Debug, Error, Info, Verbose, Warning}
import com.jacoby6000.cloneherodb.syntax.Shows
import enumeratum.values.{IntEnum, IntEnumEntry}

import scalaz.{Order, Show}

trait Logger[F[_]] {
  def log(a: Shows, level: LogLevel): F[Unit]

  def error(a: Shows): F[Unit] = log(a, Error)
  def info(a: Shows): F[Unit] = log(a, Info)
  def debug(a: Shows): F[Unit] = log(a, Debug)
  def verbose(a: Shows): F[Unit] = log(a, Verbose)
  def warning(a: Shows): F[Unit] = log(a, Warning)
}

sealed abstract class LogLevel(val value: Int, val name: String) extends IntEnumEntry
object LogLevel extends IntEnum[LogLevel] {
  implicit val logLevelOrder = Order.fromScalaOrdering[Int].contramap[LogLevel](_.value)
  implicit val logLevelShow = Show.show[LogLevel](_.name)

  case object Debug extends LogLevel(0, "DEBUG")
  case object Verbose extends LogLevel(1, "VERBOSE")
  case object Info extends LogLevel(2, "INFO")
  case object Warning extends LogLevel(3, "WARNING")
  case object Error extends LogLevel(4, "ERROR")

  val values = findValues
}

