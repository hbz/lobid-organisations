name := """lobid-organisations"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "2.3.2"
      // otherwise javaWs won't work
      exclude ("io.netty", "netty"),
  "com.github.jsonld-java" % "jsonld-java" % "0.5.0" % Test,
  "org.culturegraph" % "metafacture-core" % "2.0.1-HBZ-SNAPSHOT",
  "org.slf4j" % "slf4j-log4j12" % "1.7.6",
  "org.xbib.elasticsearch.plugin" % "elasticsearch-plugin-bundle" % "2.3.2.0"
)

resolvers += Resolver.mavenLocal

resourceDirectory in Test := baseDirectory.value / "test" / "transformation"
