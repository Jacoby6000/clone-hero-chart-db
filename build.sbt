import java.nio.file.Paths

import com.jacoby6000.cloneherodb.build.settings._
import com.jacoby6000.cloneherodb.config._

lazy val server = (project in file("."))
  .dependsOn(config)
  .aggregate(config)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonSettings)
  .settings(databaseSettings())
  .settings(
    libraryDependencies ++= rootProjectDependencies,
    scalacOptions ++= Seq(
      "-Ysysdef", "",
      "-Ypredef", "com.jacoby6000.cloneherodb._"
    )
  )

// configuration sub-project build located in <clone-hero-chart-db>/project/metabuild.sbt
lazy val config = ProjectRef(file("project"), "config")



def databaseSettings() = {
  lazy val dbConfig =
    loadCloneHeroDbConfig(Paths.get("src/main/resources/reference.conf"))
      .leftMap(failuresToErrorMessage)
      .leftMap("\n\n" + _)
      .map(_.database)
      .fold(sys.error(_), identity)

  lazy val dbHost = dbConfig.host
  lazy val dbPort = dbConfig.port
  lazy val db = dbConfig.databaseName
  lazy val user = dbConfig.username

  Seq(
    flywayUrl := s"jdbc:postgresql://$dbHost:$dbPort/$db",
    flywayUser := user
  )
}
