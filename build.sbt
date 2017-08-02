import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """todo"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

javaOptions in Test += "-Dconfig.file=conf/test.conf"

libraryDependencies ++= Seq(
  guice,
  "org.flywaydb" %% "flyway-play" % "4.0.0",
  "org.postgresql" % "postgresql" % "42.1.1",
  "org.postgresql" % "postgresql" % "42.1.1",
  "org.scalikejdbc" %% "scalikejdbc" % "3.0.1",
  "org.scalikejdbc" %% "scalikejdbc-config" % "3.0.1",
  "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "3.0.1",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.6.0",
  "org.scalikejdbc" %% "scalikejdbc-test" % "3.0.1" % "test",
  "io.swagger" %% "swagger-play2" % "1.6.0",
  "org.mockito" % "mockito-core" % "2.7.22" % Test,
  "com.h2database" % "h2" % "1.4.196" % Test,
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