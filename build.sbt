import org.fusesource.scalate.ScalatePlugin.ScalateKeys._
import org.scalatra.sbt._
import sbt.Keys.{libraryDependencies, name, parallelExecution}

val scalatraVersion = "2.5.0"
val json4sVersion = "3.5.1"
val slickVersion = "3.2.0"

val dependencies = libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % scalatraVersion,
  "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test",
  "org.scalatra" %% "scalatra-json" % scalatraVersion,
  "org.scalatra" %% "scalatra-auth" % scalatraVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "mysql" % "mysql-connector-java" % "5.1.23",
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalaz" %% "scalaz-core" % "7.2.10"
)

lazy val travelplanner = (project in file("."))
  .enablePlugins(JettyPlugin)
  .settings(
    organization := "nom.bruno",
    name := "Travel Planner",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.12.1",
    parallelExecution in Test := false,
    scalateTemplateConfig in Compile := {
      val base = (sourceDirectory in Compile).value
      Seq(
        TemplateConfig(
          base / "webapp" / "WEB-INF" / "templates",
          Seq.empty, /* default imports should be added here */
          Seq(
            Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
          ), /* add extra bindings here */
          Some("templates")
        )
      )
    },
    resolvers += Classpaths.typesafeReleases
  )
  .settings(dependencies)
  .settings(ScalatraPlugin.scalatraSettings: _*)
  .settings(scalateSettings: _*)
  .settings(debugPort in Jetty := 5005)



