import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """todo"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  guice,
  "org.flywaydb" %% "flyway-play" % "4.0.0",
  "org.postgresql" % "postgresql" % "42.1.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test)

dockerCommands := Seq(
  Cmd("FROM", "openjdk:latest"),
  Cmd("RUN", "apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*"),
  Cmd("WORKDIR", "/opt/docker"), Cmd("ADD", "opt", "/opt"),
  Cmd("RUN", "chown -R daemon:daemon ."),
  Cmd("USER", "daemon"),
  ExecCmd("ENTRYPOINT", "bin/quiz-server"),
  ExecCmd("CMD")
)