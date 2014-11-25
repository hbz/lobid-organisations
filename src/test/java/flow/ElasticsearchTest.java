package flow;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@SuppressWarnings("javadoc")
public class ElasticsearchTest {

	protected static Node node;
	protected static Client client;

	@BeforeClass
	public static void makeIndex() throws IOException {
		node = nodeBuilder().local(true).node();
		client = node.client();
		transformData();
		prepareIndexing(client);
	}

	@AfterClass
	public static void closeElasticSearch() {
		client.close();
		node.close();
	}

	public static void transformData() {
		Enrich.processSample();
	}

	public static void prepareIndexing(Client indexClient) throws IOException {
		Index.createEmptyIndex(indexClient);
		Index.indexData(indexClient);
		indexClient.admin().indices().refresh(new RefreshRequest()).actionGet();
	}
}
