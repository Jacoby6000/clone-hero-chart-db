import com.jacoby6000.cloneherodb.build.settings._

val config =
  project
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.pureconfig" %% "pureconfig" % "0.8.0" // configuration
      )
    )

val build = (project in file("."))
  .dependsOn(config)

