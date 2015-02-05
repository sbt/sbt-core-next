import com.typesafe.sbt.SbtGit._

val serializationVersion = "0.1.0-M2"
val serialization = "org.scala-sbt" %% "serialization" % serializationVersion
val sbtMothership = "org.scala-sbt" % "sbt" % "0.13.7"

val scala210Version = "2.10.4"
// val scala211Version = "2.11.5"

lazy val commonSettings = Util.settings ++ versionWithGit ++ Seq(
  organization := "org.scala-sbt",
  git.baseVersion := "0.1.0",
  isSnapshot := true,
  version := {
    val old = version.value
    if (isSnapshot.value) old
    else git.baseVersion.value
  },
  scalaVersion := scala210Version,
  crossScalaVersions := Seq(scala210Version)
)

lazy val root = (project in file(".")).
  aggregate(coreNext, sbtCoreNext).
  settings(commonSettings: _*).
  settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )

lazy val coreNext = (project in file("library")).
  settings(commonSettings: _*).
  settings(
    name := "Core Next",
    libraryDependencies ++= Seq(serialization, sbtMothership % "provided")
  )

lazy val sbtCoreNext = (project in file("sbt-core-next")).
  dependsOn(coreNext).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    name := "sbt-core-next"
  )
