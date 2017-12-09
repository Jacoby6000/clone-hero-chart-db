package com.jacoby6000.cloneherodb.build

import sbt._
import sbt.Keys._

object settings {

  val http4sVersion = "0.18.0-M5"
  val doobieVersion = "0.5.0-M9"
  val enumeratumVersion = "1.5.12"
  val tsecVersion = "0.0.1-M5"
  val argonautVersion = "6.2"

  val http4sDependencies = Seq(
      "org.http4s" %% "http4s-dsl"          % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion
  )

  val jsonDependencies = Seq(
    "io.argonaut"  %% "argonaut"            % argonautVersion,
    "io.argonaut"  %% "argonaut-scalaz"     % argonautVersion,
    "org.http4s"   %% "http4s-argonaut"     % http4sVersion,
    "com.beachape" %% "enumeratum-argonaut" % enumeratumVersion
  )

  val databaseDependencies = Seq(
    "org.tpolecat" %% "doobie-core"      % doobieVersion,
    "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
    "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "it"
  )

  val testingDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  )

  val crytoDependencies = Seq(
    "io.github.jmcardon" %% "tsec-http4s" % tsecVersion
  )

  val configurationDependencies = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.8.0"
  )

  val sanityDependencies = Seq(
    "org.scalaz"     %% "scalaz-core" % "7.2.17",          // FP
    "com.codecommit" %% "shims"       % "1.0",             // cats/scalaz compat
    "com.beachape"   %% "enumeratum"  % enumeratumVersion  // enums
  )

  val rootProjectDependencies =
    http4sDependencies ++
      jsonDependencies ++
      databaseDependencies ++
      testingDependencies ++
      crytoDependencies ++
      configurationDependencies ++
      sanityDependencies

  lazy val commonSettings = Seq(
    scalaVersion := "2.12.4",

    libraryDependencies ++= sanityDependencies,

    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),

    scalacOptions ++= Seq(
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8",                // Specify character encoding used by source files.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
      "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
      "-language:higherKinds",             // Allow higher-kinded types
      "-language:implicitConversions",     // Allow definition of implicit functions called views
      "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
      "-Xfuture",                          // Turn on future language features.
      "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
      "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
      "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
      "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
      "-Xlint:option-implicit",            // Option.apply used implicit view.
      "-Xlint:package-object-classes",     // Class or object defined in package object.
      "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
      "-Xlint:unsound-match",              // Pattern match may not be typesafe.
      "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification",             // Enable partial unification in type constructor inference
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen",              // Warn when numerics are widened.
      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
    ),

    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")
  )

}