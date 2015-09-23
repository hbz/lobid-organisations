package flow;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@SuppressWarnings("javadoc")
public abstract class ElasticsearchTest {

	protected static Node node;
	protected static Client client;

	@BeforeClass
	public static void makeIndex() throws IOException {
		node = nodeBuilder().local(true).node();
		client = node.client();
		transformData();
		prepareIndexing(client);
		indexData(client);
	}

	@AfterClass
	public static void closeElasticSearch() {
		client.close();
		node.close();
	}

	public static void transformData() throws IOException {
		EnrichSample.processSample(Constants.TEST_RESOURCES_PATH
				+ Constants.OUTPUT_PATH + "enriched.out.json");
	}

	public static void prepareIndexing(final Client aIndexClient)
			throws IOException {
		Index.createEmptyIndex(aIndexClient);
	}

	public static void indexData(final Client aIndexClient) throws IOException {
		Index.indexData(aIndexClient, Constants.TEST_RESOURCES_PATH
				+ Constants.OUTPUT_PATH + "enriched.out.json");
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
