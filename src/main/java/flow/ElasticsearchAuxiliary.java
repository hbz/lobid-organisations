package flow;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

@SuppressWarnings("javadoc")
public class ElasticsearchAuxiliary {

	// FILES & URIs
	protected static final String SIGEL_DUMP_LOCATION =
			"src/main/resources/input/sigel.xml";
	protected static final String DBS_LOCATION =
			"src/main/resources/input/dbs.csv";
	protected static final String SIGEL_DNB_REPO =
			"http://gnd-proxy.lobid.org/oai/repository";

	// ELASTICSEARCH SETTINGS
	protected static final String ES_CLUSTER = "organisation-cluster";
	protected static final String ES_INDEX = "organisations";
	protected static final String ES_TYPE = "organisation";
	protected static final String SERVER_NAME = "weywot2.hbz-nrw.de";

	// ELASTICSEARCH COMPONENTS
	protected static final InetSocketTransportAddress NODE_1 =
			new InetSocketTransportAddress(SERVER_NAME, 9300);
	protected static final Builder CLIENT_SETTINGS = ImmutableSettings
			.settingsBuilder().put("cluster.name", ES_CLUSTER)
			.put("index.name", ES_INDEX);
	private static final TransportClient TC = new TransportClient(CLIENT_SETTINGS
			.put("client.transport.sniff", false)
			.put("client.transport.ping_timeout", 20, TimeUnit.SECONDS).build());
	protected static final Client ES_CLIENT = TC.addTransportAddress(NODE_1);
}
