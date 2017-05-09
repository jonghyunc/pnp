organization := "org.allenai"

name := "pnp"

description := "Library for probabilistic neural programming"

version := "0.1.2"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.2.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.3",
  "com.google.guava" % "guava" % "17.0",
  "com.jayantkrish.jklol" % "jklol" % "1.2.1",
  "io.spray" %%  "spray-json" % "1.3.3",
  "net.sf.jopt-simple" % "jopt-simple" % "4.9",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
