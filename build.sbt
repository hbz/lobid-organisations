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
  "com.github.jsonld-java" % "jsonld-java" % "0.4.1",
  "com.github.jsonld-java" % "jsonld-java-jena" % "0.4.1" exclude("org.slf4j", "slf4j-log4j12"),
  "org.apache.jena" % "jena-arq" % "2.9.3",
  "org.metafacture" % "morph" % "5.6.0",
  "org.metafacture" % "elasticsearch" % "5.6.0",
  "org.metafacture" % "json" % "5.6.0",  
  "org.metafacture" % "csv" % "5.6.0",  
  "org.metafacture" % "io" % "5.6.0",  
  "org.metafacture" % "triples" % "5.6.0",  
  "org.metafacture" % "biblio" % "5.6.0",   
  "org.metafacture" % "xml" % "5.6.0",   
  "org.metafacture" % "framework" % "5.6.0", 
  "org.xbib.elasticsearch.plugin" % "elasticsearch-plugin-bundle" % "2.3.2.0",
  "com.jayway.jsonpath" % "json-path" % "2.2.0",
  "net.java.dev.jna" % "jna" % "4.1.0",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.13"
)

resolvers += Resolver.mavenLocal

resourceDirectory in Test := baseDirectory.value / "test" / "transformation"
