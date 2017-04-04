import org.scalatra.sbt._
import sbt.Keys.{libraryDependencies, name, parallelExecution}

val scalatraVersion = "2.5.0"
val json4sVersion = "3.5.1"
val slickVersion = "3.2.0"

val dependencies = libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % scalatraVersion,
  "org.scalatra" %% "scalatra-json" % scalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test" exclude("org.mockito", "mockito-all"),
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalaz" %% "scalaz-core" % "7.2.10",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container",
  "mysql" % "mysql-connector-java" % "5.1.23",
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "com.google.inject" % "guice" % "4.1.0",
  "org.mockito" % "mockito-core" % "2.7.21" % "test"
)

val commonSettings = Seq(
  organization := "nom.bruno",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.1",
  resolvers += Classpaths.typesafeReleases
)

lazy val baseTravelPlanner = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(api, integration)

lazy val api = (project in file("api"))
  .enablePlugins(JettyPlugin)
  .settings(name := "api")
  .settings(commonSettings: _*)
  .settings(dependencies)
  .settings(ScalatraPlugin.scalatraSettings: _*)
  .settings(debugPort in Jetty := 5005)

lazy val integration = (project in file("integration"))
  .enablePlugins(JettyPlugin)
  .settings(
    name := "integration",
    parallelExecution in Test := false
  )
  .settings(commonSettings: _*)
  .settings(dependencies)
  .dependsOn(api % "compile->compile;test->test")

onLoad in Global := (onLoad in Global).value andThen (Command.process("project api", _))
