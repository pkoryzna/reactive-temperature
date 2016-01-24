name := "reactive-temperature"

version := "1.1"

scalaVersion := "2.11.7"

libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.2",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.0.2",
  "com.typesafe" % "config" % "1.3.0",
  "com.lihaoyi" %% "scalatags" % "0.5.2",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0.2",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)