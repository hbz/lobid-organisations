package flow;

@SuppressWarnings("javadoc")
public class Constants {

	protected static final String MAIN_RESOURCES_PATH = "src/main/resources/";

	protected static final String OUTPUT_PATH = "output/";
	protected static final String INPUT_PATH = "input/";

	protected static final String SIGEL_DUMP_TOP_LEVEL_TAG = "collection";
	protected static final String SIGEL_DUMP_ENTITY = "record";

	protected static final String SIGEL_UPDATE_TOP_LEVEL_TAG = "/harvest";
	protected static final String SIGEL_UPDATE_ENTITY = "metadata";

	protected static final String SIGEL_XPATH =
			"/*[local-name() = 'record']/*[local-name() = 'global']/*[local-name() = 'tag'][@id='008H']/*[local-name() = 'subf'][@id='e']";
}
