name := "raft-workshop"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "org.wvlet.airframe" %% "airframe-log" % "19.6.1",
  "com.typesafe.akka" %% "akka-actor" % "2.5.23"
)


PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
