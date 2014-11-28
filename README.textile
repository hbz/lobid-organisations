h1. About

Experimental new data transformation workflows for the lobid-organisations data set based on "Metafacture":https://github.com/culturegraph/metafacture-core/wiki.

Currently transforms two data sets from Pica-XML and CSV to JSON-LD for Elasticsearch indexing. Data of two or more entries are merged if the DBS IDs (INR) of the entries are identical. The resulting data is used to build an index in Elasticsearch.

_Note: for the currently productive transformations see "https://github.com/lobid/lodmill":https://github.com/lobid/lodmill_

h1. Overview

This repo is an implementation of a _Lobid Dataset Module_. See its role in the _Lobid Macro Architecture_:

!{width:50%}architecture.png!  

h1. Setup

h2. Build

"!https://secure.travis-ci.org/hbz/lobid-organisations.png?branch=master!":https://travis-ci.org/hbz/lobid-organisations

Prerequisites: Java 8, Maven 3; verify with @mvn -version@

Create and change into a folder where you want to store the projects:

* @mkdir ~/git ; cd ~/git@

Build the hbz metafacture-core fork:

* @git clone https://github.com/hbz/metafacture-core.git@
* @cd metafacture-core@
* @mvn clean install -DskipTests@
* @cd ..@

Build lobid-organisations:

* @git clone https://github.com/hbz/lobid-organisations.git@
* @cd lobid-organisations@
* @mvn clean install@

See the @.travis.yml@ file for details on the CI config used by Travis.

h2. Tests

The build described above executes tests of the Metamorph transformations and the Elasticsearch indexing.

The Metamorph tests are defined in XML files with a @test_@ prefix corresponding to the tested Metamorph files:

in @src/main/resources@:

@morph-dbs.xml@
@morph-enriched.xml@
@morph-sigel.xml@

in @src/test/resources@:

@test_morph-dbs.xml@
@test_morph-enriched.xml@
@test_morph-sigel.xml@

For details, see the "Metamorph testing framework documentation":https://github.com/culturegraph/metafacture-core/wiki/Testing-Framework-for-Metamorph.

The Elasticsearch tests are defined in in @src/test/java/TestNGram.java@.

h2. Eclipse

The processing pipelines are written in Java, the actual transformation logic and tests are written in XML. Both can be comfortably edited using Eclipse, which provides content assist (auto-suggest), Maven support, and direct execution of the tests and the transformation.

* Download and run the "Eclipse Java IDE":https://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/lunar (at least version 4.4, Luna), close the welcome screen and import metafacture-core and lobid-resources: 
* @File@ -> @Import...@ -> @Maven@ -> @Existing Maven Projects...@ -> @Next@ -> @Browse...@ -> select @~/git@ -> @Finish@
* Follow the instructions for installing additional plugins and restart Eclipse when it asks you to
* Set up the XML schemas for content assist and documentation while editing metamorph and metamorph-test files:
* @Window@ (on Mac: @Eclipse@) -> @Preferences@ -> @XML@ -> @XML Catalog@
* select @User Specified Entries@ -> @Add...@ -> @Workspace...@ -> @metafacture-core/src/main/resources/schemata/metamorph.xsd@
* repeat previous step for @metamorph-test.xsd@ in the same location

h1. Data

h2. Workflow

The source data sets are the _Sigelverzeichnis_ ('Sigel', format: PicaPlus-XML) and the _Deutsche Bibliotheksstatistik_ ('DBS', format: CSV). The transformation is implemented by a pipeline with 3 logical steps:

* Preprocess Sigel data, use DBS ID as record ID; if no DBS ID is available, use ISIL; in this step, updates are downloaded for the time period from base dump creation until today
* Preprocess DBS data, use DBS ID as record ID
* Combine all data from Sigel and DBS:
** Merge Sigel and DBS entries that have identical DBS IDs
** Entries with a unique DBS ID or without DBS ID are integrated as well -- they are not merged with any other entry
** The entries in the resulting data set have a URI with their ISIL as ID (e.g., http://data.lobid.org/organisations/DE-9). If no ISIL is available, a Pseudo-ISIL is generated consisting of the string 'DBS-' and the DBS ID (e.g., http://data.lobid.org/organisations/DBS-GX848). 

Each of these steps has a corresponding Java class, Morph definition, and output file. 

Finally, the data can be indexed in Elasticsearch using a separate Java class (only works hbz internally). The ID of an organisation is represented as a URI (e.g., http://data.lobid.org/organisations/DE-9). However, when building up the index, the organisations are given the last bit of this URI only as Elasticsearch IDs (e.g., DE-9). Thus, Elasticsearch-internally, the organisations can be accessed via their ISIL or Pseudo-ISIL.

h2. Downloads

Optional: replace the sample data with the full input data dumps (only works hbz internally!):

* @cd src/main/resources/input/@
* @wget http://quaoar1.hbz-nrw.de:7001/assets/data/dbs.zip; unzip dbs.zip@
* @wget http://quaoar1.hbz-nrw.de:7001/assets/data/sigel.xml@

h2. Transform

h3. Sample data transformation

You can run the transformation with the sample data that is included in this repository. To do so, go to the project root and run EnrichSample: 
@cd ../../../../@
@mvn exec:java -Dexec.mainClass="flow.EnrichSample"@ 

h3. Full data transformation (only works hbz internally!)

If you run the transformation with the full data (see above for downloads), the application will download additional updates for the Sigel data. These downloads comprise the data from a given date until today. They are split into smaller intervals of several days, you can specify the size of these intervals. Thus, you will have to pass two arguments to the main method of Enrich: (1) the date from which the updates start (usually the date of the base dump creation) and (2) the interval size in days (must not be too large). With the Sigel base dump that is available from the download link above (created in June 2013) and an interval size of 100 days for each update download, you run the following to transform the data:
@cd ../../../../@
@mvn exec:java -Dexec.mainClass="flow.Enrich" -Dexec.args="'2013-06-01' '100'"@

h2. Index

The Elasticsearch index is constructed from the output of the transformation. Before building the index, the application will check for the minimum size of the transformation output (file: src/main/resources/output/enriched.out.json). This is done to prevent building up an index that only contains part of the available data or no data at all (e.g. if something goes wrong during the transformation, the result may be an empty file). This minimum size threshold is specified as an argument to the main method of the class Index.

Run the indexing and pass a minimum file size in bytes (indexing only works hbz internally!):

* For the sample data:
@mvn exec:java -Dexec.mainClass="flow.Index" -Dexec.args="5000"@

* For the full data:
@mvn exec:java -Dexec.mainClass="flow.Index" -Dexec.args="25000000"@

h2. Query

Query the resulting index (only works hbz internally!):

* @curl -XGET 'http://weywot2.hbz-nrw.de:9200/organisations/_search?q=name:hbz+OR+altLabel:hbz'; echo@

For details on the various options see the "query string syntax documentation":http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax.

Get a specific record by DE-9:

* @curl -XGET 'http://weywot2.hbz-nrw.de:9200/organisations/organisation/DE-9'; echo@

Exclude the metadata (you can paste the resulting document into the "JSON-LD Playground":http://json-ld.org/playground/ for conversion tests etc.):

* @curl -XGET 'http://weywot2.hbz-nrw.de:9200/organisations/organisation/DE-9/_source'; echo@

For details on the various options see the "GET API documentation":http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-get.html.

h2. Web

The @web/@ directory contains a basic web app implemented with Play to serve the JSON-LD context as @application/ld+json@ with CORS support. This is required to use the JSON-LD from third party clients, e.g. the "JSON-LD Playground":http://json-ld.org/playground/. It also provides proxy routes for Elasticsearch queries via HTTP (see index page of the web app for details).

Set up the Play application:

* @cd ~@
* @wget http://downloads.typesafe.com/typesafe-activator/1.2.10/typesafe-activator-1.2.10-minimal.zip@
* @unzip typesafe-activator-1.2.10-minimal.zip@
* @cd git/lobid-organisations/web@
* @~/activator-1.2.10-minimal/activator run@

Open @http://localhost:9000/organisations@

Load the web application into Eclipse:

* @Ctrl+D@
* @eclipse@
* In Eclipse: @File@ -> @Import...@ -> @Existing Projects into Workspace...@ -> @Next@ -> @Browse...@ -> select root directory: @~/git/lobid-organisations/web@ -> @Finish@

The web application can also be accessed via http://data.lobid.org/organisations.

h1. License

Eclipse Public License: "http://www.eclipse.org/legal/epl-v10.html":http://www.eclipse.org/legal/epl-v10.html
