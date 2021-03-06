import org.scalatra.sbt._
import sbt.Keys.{libraryDependencies, parallelExecution}

val scalatraVersion = "2.5.0"
val json4sVersion = "3.5.1"
val slickVersion = "3.2.0"

val logDependencies = libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

val dependencies = libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % scalatraVersion,
  "org.scalatra" %% "scalatra-json" % scalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test" exclude("org.mockito", "mockito-all"),
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container",
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "org.mockito" % "mockito-core" % "2.7.21" % "test"
)

val akkaHttpVersion = "10.0.5"

val akkaDependencies = libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.mockito" % "mockito-core" % "2.7.21" % "test"
)

val travelPlannerApiDependencies = libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.10",
  "com.google.inject" % "guice" % "4.1.0",
  "mysql" % "mysql-connector-java" % "5.1.23",
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "mysql" % "mysql-connector-java" % "5.1.23",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
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
  .settings(addCommandAlias("test", ";api/test;scalatra/test;akkaHttp/test;apiIntegration/test;scalatraIntegration/test;akkaHttpIntegration/test"): _*)
  .aggregate(api, apiIntegration, scalatra, scalatraIntegration, akkaHttp, akkaHttpIntegration)

lazy val commonResources = (project in file("commonResources"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)

lazy val api = (project in file("api"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .settings(travelPlannerApiDependencies)

lazy val apiIntegration = (project in file("apiIntegration"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .dependsOn(commonResources % "test->test")
  .dependsOn(api % "compile->compile;test->test")

lazy val scalatra = (project in file("scalatra"))
  .enablePlugins(JettyPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings: _*)
  .settings(dependencies)
  .settings(logDependencies)
  .settings(ScalatraPlugin.scalatraSettings: _*)
  .settings(debugPort in Jetty := 5005)
  .dependsOn(commonResources)
  .dependsOn(api)

lazy val scalatraIntegration = (project in file("scalatraIntegration"))
  .enablePlugins(JettyPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    parallelExecution in Test := false
  )
  .settings(commonSettings: _*)
  .settings(dependencies)
  .dependsOn(scalatra % "compile->compile;test->test")
  .dependsOn(commonResources % "test->test")

lazy val akkaHttp = (project in file("akka-http"))
  .settings(commonSettings: _*)
  .settings(akkaDependencies)
  .settings(logDependencies)
  .dependsOn(commonResources)
  .dependsOn(api)

lazy val akkaHttpIntegration = (project in file("akka-httpIntegration"))
  .disablePlugins(RevolverPlugin)
  .settings(
    parallelExecution in Test := false
  )
  .settings(commonSettings: _*)
  .dependsOn(akkaHttp % "compile->compile;test->test")
  .dependsOn(commonResources % "test->test")

onLoad in Global := (onLoad in Global).value andThen (Command.process("project scalatra", _))
