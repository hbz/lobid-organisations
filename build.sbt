name := """lobid-organisations"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "2.3.1"
      // otherwise javaWs won't work
      exclude ("io.netty", "netty"),
  "org.elasticsearch.module" % "lang-groovy" % "2.3.1",
  "org.codehaus.groovy" % "groovy-all" % "2.4.4" classifier "indy",
  "com.github.jsonld-java" % "jsonld-java" % "0.5.0" % Test,
  "org.culturegraph" % "metafacture-core" % "2.0.1-HBZ-SNAPSHOT",
  "org.slf4j" % "slf4j-log4j12" % "1.7.6"
)

resolvers += Resolver.mavenLocal

resourceDirectory in Test := baseDirectory.value / "test" / "transformation"
