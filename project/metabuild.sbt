import com.jacoby6000.cloneherodb.build.settings._

val config =
  project
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= configurationDependencies
    )

val build = (project in file("."))
  .dependsOn(config)

