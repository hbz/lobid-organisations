package flow;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@SuppressWarnings("javadoc")
public abstract class ElasticsearchTest {

	protected static Node node;
	protected static Client client;
	protected static Node geoNode;
	protected static Client geoClient;

	@BeforeClass
	public static void makeIndex() throws IOException {
		node = nodeBuilder().settings(Settings.builder().put("path.home", "."))
				.local(true).node();
		client = node.client();
		setupLocalGeodataExample();
		transformData();
		prepareIndexing(client, Constants.ES_INDEX,
				Constants.MAIN_RESOURCES_PATH + "index-settings.json");
		indexData(client, Constants.TEST_RESOURCES_PATH + Constants.OUTPUT_PATH
				+ "enriched.out.json", Constants.ES_INDEX);
	}

	@AfterClass
	public static void closeElasticSearch() {
		client.close();
		node.close();
		geoClient.close();
		geoNode.close();
	}

	private static void setupLocalGeodataExample() throws IOException {
		geoNode = nodeBuilder().local(true)
				.settings(Settings.builder().put("path.home", ".")).node();
		geoClient = geoNode.client();
		String geoIndex = "geodata";
		prepareIndexing(geoClient, geoIndex, null);
		indexData(geoClient, Constants.TEST_RESOURCES_PATH + Constants.INPUT_PATH
				+ "AA603_geodata.json", geoIndex);
	}

	public static void transformData() throws IOException {
		EnrichSample.processSample(Constants.TEST_RESOURCES_PATH
				+ Constants.OUTPUT_PATH + "enriched.out.json");
	}

	public static void prepareIndexing(final Client aIndexClient,
			final String aIndex, final String aPathToIndexSettings)
			throws IOException {
		Index.createEmptyIndex(aIndexClient, aIndex, aPathToIndexSettings);
	}

	public static void indexData(final Client aIndexClient, final String aFile,
			final String aIndex) throws IOException {
		Index.indexData(aIndexClient, aFile, aIndex);
	}

	public static SearchResponse exactSearch(final String aField,
			final String aValue) {
		final SearchResponse responseOfSearch =
				client.prepareSearch(Constants.ES_INDEX).setTypes(Constants.ES_TYPE)
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.termQuery(aField, aValue)).execute()
						.actionGet();
		return responseOfSearch;
	}

	public static SearchResponse search(final String aField,
			final String aValue) {
		SearchResponse responseOfSearch =
				client.prepareSearch(Constants.ES_INDEX).setTypes(Constants.ES_TYPE)
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.matchQuery(aField, aValue)).execute()
						.actionGet();
		return responseOfSearch;
	}

}
