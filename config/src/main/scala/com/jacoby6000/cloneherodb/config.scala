package com.jacoby6000.cloneherodb

import java.nio.file.Path

import pureconfig._
import pureconfig.error.{ConfigReaderFailures, ConvertFailure}

import scala.concurrent.duration.FiniteDuration
import scalaz._
import Scalaz._

object config {

  def loadCloneHeroDbConfig(configPath: Path): ConfigReaderFailures \/ CloneHeroDbConfiguration =
    loadConfig[CloneHeroDbConfiguration](
      path = configPath,
      namespace = "clone-hero-db"
    ).disjunction

  def failuresToErrorMessage(configReaderFailures: ConfigReaderFailures): String =
    configReaderFailures.toList.map {
      case err: ConvertFailure => err.path + ": " + err.description
      case err => err.location.cata(v => v.description, "Config path unknown") + ": " + err.description
    }.foldLeft("Failed to read configuration:\n") { (acc, err) =>
      acc + "\t" + err + "\n"
    }

  case class CloneHeroDbConfiguration(
    database: DatabaseConfiguration,
    google: GoogleApiConfiguration,
    api: ApiConfiguration
  )

  case class GoogleApiConfiguration(
    oauth: GoogleOAuthApiConfiguration,
    drive: GoogleDriveApiConfiguration
  )

  case class GoogleOAuthApiConfiguration(
    newTokenAfter: FiniteDuration,
    credentialsJsonLocation: String
  )

  case class GoogleDriveApiConfiguration(
    requestThrottleDelay: FiniteDuration
  )

  case class DatabaseConfiguration(
    host: String,
    port: Int,
    databaseName: String,
    username: String,
    password: Option[String]
  )

  case class ApiConfiguration(bindTo: String, port: Int)
}
