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

val akkaHttpVersion = "10.0.5"

val akkaDependencies = libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.google.inject" % "guice" % "4.1.0"
)

val commonSettings = Seq(
  organization := "nom.bruno",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.1",
  resolvers += Classpaths.typesafeReleases
)

lazy val baseTravelPlanner = (project in file("."))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .aggregate(api, integration, apiAkka)

lazy val api = (project in file("api"))
  .enablePlugins(JettyPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(name := "api")
  .settings(commonSettings: _*)
  .settings(dependencies)
  .settings(ScalatraPlugin.scalatraSettings: _*)
  .settings(debugPort in Jetty := 5005)

lazy val integration = (project in file("integration"))
  .enablePlugins(JettyPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    name := "integration",
    parallelExecution in Test := false
  )
  .settings(commonSettings: _*)
  .settings(dependencies)
  .dependsOn(api % "compile->compile;test->test")

lazy val apiAkka = (project in file("api-akka-http"))
  .settings(commonSettings: _*)
  .settings(akkaDependencies)

onLoad in Global := (onLoad in Global).value andThen (Command.process("project api", _))
