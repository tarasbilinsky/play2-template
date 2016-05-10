name := "play"
organization := "net.oltiv"
version := "0.0.0.1"

lazy val `play` = (project in file(".")).enablePlugins(PlayScala,PlayEbean)

scalaVersion := "2.11.8"
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
scalacOptions ++= Seq("-feature","-unchecked","-deprecation")
javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,

  "net.oltiv" % "scala-ebean-macros" % "0.1.8",

  "mysql" % "mysql-connector-java" % "5.1.38",

  "com.amazonaws" % "aws-java-sdk" % "1.10.66",

  "com.sun.mail" % "mailapi" % "1.5.5",
  "com.sun.mail" % "smtp" % "1.5.5",
  "ch.qos.logback" % "logback-access" % "1.1.7",

  "org.apache.commons" % "commons-email" % "1.4",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "org.avaje" % "avaje-agentloader" % "2.1.2" % "test"
)


sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value
TwirlKeys.templateImports in Compile ++= Seq(
  "models._",
  "base.MyConfigImplicit.MyConfig",
  "base.controllers.EnvironmentAll",
  "base.controllers.ControllerCrud"
)

includeFilter in (Assets, LessKeys.less) := "dev.less" | "main.less"

ivyLoggingLevel := UpdateLogging.Quiet







