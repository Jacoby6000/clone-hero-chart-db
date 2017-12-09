import com.jacoby6000.cloneherodb.build.settings._

lazy val server = (project in file("."))
  .dependsOn(config)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonSettings)
  .settings(
    resolvers += "jmcardon at bintray" at "https://dl.bintray.com/jmcardon/tsec",

    libraryDependencies ++= Seq(
      // http things
      "org.http4s" %% "http4s-dsl"          % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,

      // JSON
      "io.argonaut"  %% "argonaut"            % argonautVersion,
      "io.argonaut"  %% "argonaut-scalaz"     % argonautVersion,
      "org.http4s"   %% "http4s-argonaut"     % http4sVersion,
      "com.beachape" %% "enumeratum-argonaut" % enumeratumVersion,

      // db access
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "it",

      // testing
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",

      // security
      "io.github.jmcardon" %% "tsec-http4s" % tsecVersion
    )
  )

lazy val config = ProjectRef(file("project"), "config")

val dbHost = "localhost"
val dbPort = 5432
val db = "clone_hero_db"
val user = "clone_hero_db"

flywayUrl := s"jdbc:postgresql://$dbHost:$dbPort/$db"
flywayUser := user

