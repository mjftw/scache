lazy val CatsEffectVersion = "2.5.1"
lazy val ScalaTestVersion = "3.2.5"

lazy val root = (project in file("."))
  .settings(
    organization := "io.github.mjftw",
    name := "scache",
    version := "0.1.0",
    scalaVersion := "2.13.4",
    scalafixOnCompile := true,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions := Seq(
      "-Ywarn-unused"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    )
  )
