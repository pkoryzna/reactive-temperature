name := "reactive-temperature"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.14",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "com.typesafe" % "config" % "1.3.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)