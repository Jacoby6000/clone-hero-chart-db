// Database migrations
resolvers += "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
