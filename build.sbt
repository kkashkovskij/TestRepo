name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).disablePlugins(PlayFilters)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.2"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies ++= Seq(evolutions, jdbc)
libraryDependencies += "org.postgresql" % "postgresql" % "42.1.2.jre6"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"

