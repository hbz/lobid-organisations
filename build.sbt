name := """lobid-organisations"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.12"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "2.3.2"
      // otherwise javaWs won't work
      exclude ("io.netty", "netty"),
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.1",
  "com.github.jsonld-java" % "jsonld-java" % "0.13.6",
  "org.apache.jena" % "jena-arq" % "3.17.0",
  "org.metafacture" % "metamorph" % "7.0.0" exclude("org.slf4j", "slf4j-simple"),
  "org.metafacture" % "metafacture-elasticsearch" % "7.0.0",
  "org.metafacture" % "metamorph-test" % "7.0.0",
  "org.metafacture" % "metafacture-json" % "7.0.0",
  "org.metafacture" % "metafacture-csv" % "7.0.0",
  "org.metafacture" % "metafacture-io" % "7.0.0",
  "org.metafacture" % "metafacture-triples" % "7.0.0",
  "org.metafacture" % "metafacture-biblio" % "7.0.0",
  "org.metafacture" % "metafacture-xml" % "7.0.0",
  "org.metafacture" % "metafacture-framework" % "7.0.0",
  "org.metafacture" % "metafacture-strings" % "7.0.0",
  "org.metafacture" % "metafix" % "7.0.0",
  "org.xbib.elasticsearch.plugin" % "elasticsearch-plugin-bundle" % "2.3.2.0",
  "com.jayway.jsonpath" % "json-path" % "2.2.0",
  "net.java.dev.jna" % "jna" % "4.1.0",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.13",
  "org.slf4j" % "slf4j-reload4j" % "1.7.36"
)

// force play to use these versions (ignoring transitive dependencies)
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.2"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.2"

resolvers += Resolver.mavenLocal

resourceDirectory in Test := baseDirectory.value / "test" / "transformation"

javacOptions ++= Seq("-source", "11", "-target", "11")

import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
