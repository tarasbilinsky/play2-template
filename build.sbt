name := "play"
organization := "net.oltiv"
version := "0.0.0.1"

lazy val `play` = (project in file(".")).enablePlugins(PlayScala,PlayEbean)

scalaVersion := "2.11.8"
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
scalacOptions ++= Seq("-feature","-unchecked","-deprecation","-Xlint:unsound-match",
  //"-Xfatal-warnings",
  //"-Ylog-classpath",
  "-Yno-adapted-args")
javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

incOptions := incOptions.value.withNameHashing(true)
updateOptions := updateOptions.value.withCachedResolution(true)


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

  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
  "org.avaje" % "avaje-agentloader" % "2.1.2" % "test",

  "com.ibm.icu" % "icu4j" % "54.1.1"
)


sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value
TwirlKeys.templateImports in Compile ++= Seq(
  "models._",
  "base.MyConfigImplicit.MyConfig",
  "base.controllers._",
  "base.controllers.RequestWrapperForTemplates._"
)

includeFilter in (Assets, LessKeys.less) := "dev.less" | "main.less"

ivyLoggingLevel := UpdateLogging.Quiet

/*
coverageEnabled := false
coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*"
coverageMinimum := 80
coverageFailOnMinimum := false
*/







