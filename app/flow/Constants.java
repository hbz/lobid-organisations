package flow;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@SuppressWarnings("javadoc")
public class Constants {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();

	// FILE PATHS
	static final String TRANSFORMATION_INPUT = "app/resources/input/";
	static final String TRANSFORMATION_OUTPUT = "app/resources/output/";
	static final String TRANSFORMATION_MORPH = "app/resources/";

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
