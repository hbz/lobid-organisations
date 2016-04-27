name := """lobid-organisations-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "2.3.1"
      // otherwise javaWs won't work
      exclude ("io.netty", "netty"),
  "com.github.jsonld-java" % "jsonld-java" % "0.5.0" % Test
)
