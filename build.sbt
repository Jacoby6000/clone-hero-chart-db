import com.jacoby6000.cloneherodb.build.settings._

lazy val server = (project in file("."))
  .dependsOn(config)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonSettings)
  .settings(
    resolvers += "jmcardon at bintray" at "https://dl.bintray.com/jmcardon/tsec",

    libraryDependencies ++= rootProjectDependencies
  )

// configuration sub-project build located in <clone-hero-chart-db>/project/metabuild.sbt
lazy val config = ProjectRef(file("project"), "config")

val dbHost = "localhost"
val dbPort = 5432
val db = "clone_hero_db"
val user = "clone_hero_db"

flywayUrl := s"jdbc:postgresql://$dbHost:$dbPort/$db"
flywayUser := user

