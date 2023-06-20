name := """lobid-organisations"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.elasticsearch" % "elasticsearch" % "2.3.2"
      // otherwise javaWs won't work
      exclude ("io.netty", "netty"),
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.1",
  "com.github.jsonld-java" % "jsonld-java" % "0.4.1",
  "com.github.jsonld-java" % "jsonld-java-jena" % "0.4.1" exclude("org.slf4j", "slf4j-log4j12"),
  "org.apache.jena" % "jena-arq" % "2.9.3",
  "org.metafacture" % "metamorph" % "5.6.0",
  "org.metafacture" % "metafacture-elasticsearch" % "5.6.0",
  "org.metafacture" % "metamorph-test" % "5.6.0",
  "org.metafacture" % "metafacture-json" % "5.6.0",
  "org.metafacture" % "metafacture-csv" % "5.6.0",
  "org.metafacture" % "metafacture-io" % "5.6.0",
  "org.metafacture" % "metafacture-triples" % "5.6.0",
  "org.metafacture" % "metafacture-biblio" % "5.6.0",
  "org.metafacture" % "metafacture-xml" % "5.6.0",
  "org.metafacture" % "metafacture-framework" % "5.6.0",
  "org.metafacture" % "metafix" % "0.4.0",
  "org.xbib.elasticsearch.plugin" % "elasticsearch-plugin-bundle" % "2.3.2.0",
  "com.jayway.jsonpath" % "json-path" % "2.2.0",
  "net.java.dev.jna" % "jna" % "4.1.0",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.13"
)

// force play to use these versions (ignoring transitive dependencies)
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.2"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.2"
dependencyOverrides += "org.apache.jena" % "jena-core" % "2.11.1"

resolvers += Resolver.mavenLocal

resourceDirectory in Test := baseDirectory.value / "test" / "transformation"
