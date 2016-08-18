logLevel := Level.Warn

resolvers ++= Seq(
  "Local Repository" at "file://"+Path.userHome.absolutePath+"/.ivy2/local/",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Maven Central" at "https://repo1.maven.org/maven2"
)

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "3.0.2")

// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")