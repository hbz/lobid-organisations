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
  "org.metafacture" % "metafacture-elasticsearch" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-json" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-csv" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-io" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-triples" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-biblio" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-xml" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-framework" % "master-SNAPSHOT",
  "org.metafacture" % "metafacture-strings" % "master-SNAPSHOT",
  "org.metafacture" % "metafix" % "master-SNAPSHOT",
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
