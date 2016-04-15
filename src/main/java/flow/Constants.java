package flow;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

@SuppressWarnings("javadoc")
public class Constants {

	// FILES & URIs
	protected static final String MAIN_RESOURCES_PATH = "src/main/resources/";
	protected static final String TEST_RESOURCES_PATH = "src/test/resources/";

	protected static final String OUTPUT_PATH = "output/";
	protected static final String INPUT_PATH = "input/";

	protected static final String SIGEL_DUMP_LOCATION = //
			INPUT_PATH + "sigel.xml";
	protected static final String DBS_LOCATION = //
			INPUT_PATH + "dbs.csv";
	protected static final String SIGEL_DNB_REPO =
			"http://gnd-proxy.lobid.org/oai/repository";

	// DATA STRUCTURE
	protected static final String SIGEL_DUMP_TOP_LEVEL_TAG = "collection";
	protected static final String SIGEL_DUMP_ENTITY = "record";
	protected static final String SIGEL_UPDATE_TOP_LEVEL_TAG = "harvest";
	protected static final String SIGEL_UPDATE_ENTITY = "metadata";
	protected static final String SIGEL_XPATH =
			"/*[local-name() = 'record']/*[local-name() = 'global']/*[local-name() = 'tag'][@id='008H']/*[local-name() = 'subf'][@id='e']";

	// ELASTICSEARCH SETTINGS
	protected static final String ES_CLUSTER = "elasticsearch";
	protected static final String ES_INDEX = "organisations";
	protected static final String ES_TYPE = "organisation";
	protected static final String SERVER_NAME = "localhost"; // "quaoar1.hbz-nrw.de";

	// ELASTICSEARCH COMPONENTS
	protected static final InetSocketTransportAddress NODE_1 =
			new InetSocketTransportAddress(new InetSocketAddress(SERVER_NAME, 9300));
	protected static final Settings CLIENT_SETTINGS =
			Settings.settingsBuilder().put("cluster.name", ES_CLUSTER)
					.put("client.transport.ping_timeout", 20, TimeUnit.SECONDS).build();
	private static final TransportClient TC =
			TransportClient.builder().settings(CLIENT_SETTINGS).build();
	protected static final Client ES_CLIENT = TC.addTransportAddress(NODE_1);

}
