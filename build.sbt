name := "activator-akka-tracing"

version := "0.3-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers += "Maven Central Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.github.levkhomich" %% "akka-tracing-core" % "0.3-SNAPSHOT" changing(),
  "com.github.levkhomich" %% "akka-tracing-spray" % "0.3-SNAPSHOT" changing(),
  "io.spray" % "spray-can" % "1.3.1",
  "io.spray" % "spray-client" % "1.3.1",
  "com.typesafe" % "config" % "1.2.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.2"
)
