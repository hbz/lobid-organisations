package flow;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@SuppressWarnings("javadoc")
public class Constants {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();

	// FILES & URIs
	// protected static final String MAIN_RESOURCES_PATH = "app/resources/";
	// protected static final String TEST_RESOURCES_PATH = "test/resources/";

	// protected static final String OUTPUT_PATH = "output/";
	// protected static final String INPUT_PATH = "input/";

	// protected static final String SIGEL_DUMP_LOCATION = //
	// INPUT_PATH + "sigel.xml";
	//
	// protected static final String DBS_LOCATION = //
	// INPUT_PATH + "dbs.csv";
	// protected static final String SIGEL_DNB_REPO =
	// "http://gnd-proxy.lobid.org/oai/repository";

	// FILE PATHS
	protected static final String TRANSFORMATION_INPUT =
			CONFIG.getString("transformation.input.path");
	protected static final String TRANSFORMATION_OUTPUT =
			CONFIG.getString("transformation.output.path");
	protected static final String TRANSFORMATION_MORPH =
			CONFIG.getString("transformation.morph.path");

	// DATA STRUCTURE
	protected static final String SIGEL_DUMP_TOP_LEVEL_TAG = "collection";
	protected static final String SIGEL_DUMP_ENTITY = "record";
	protected static final String SIGEL_UPDATE_TOP_LEVEL_TAG = "harvest";
	protected static final String SIGEL_UPDATE_ENTITY = "metadata";
	protected static final String SIGEL_XPATH =
			"/*[local-name() = 'record']/*[local-name() = 'global']/*[local-name() = 'tag'][@id='008H']/*[local-name() = 'subf'][@id='e']";

	// ELASTICSEARCH SETTINGS
	protected static final String ES_INDEX = CONFIG.getString("index.es.name");
	protected static final String ES_TYPE = CONFIG.getString("index.es.type");

}
