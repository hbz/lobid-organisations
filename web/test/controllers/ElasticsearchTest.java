package controllers;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@SuppressWarnings("javadoc")
public abstract class ElasticsearchTest {

	public static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();

	protected static Client client = Index.CLIENT;
	protected static Node geoNode;
	protected static Client geoClient;

	@BeforeClass
	public static void makeIndex() throws IOException {
		setupLocalGeodataExample();
		prepareIndexing(client, CONFIG.getString("index.es.name"),
				"conf/index-settings.json");
		indexData(client, "resources/enriched-test.json",
				CONFIG.getString("index.es.name"));
	}

	@AfterClass
	public static void closeElasticSearch() {
		client.close();
		geoClient.close();
		geoNode.close();
	}

	private static void setupLocalGeodataExample() throws IOException {
		geoNode = nodeBuilder().local(true)
				.settings(Settings.builder().put("path.home", ".")).node();
		geoClient = geoNode.client();
		String geoIndex = "geodata";
		prepareIndexing(geoClient, geoIndex, null);
		indexData(geoClient, "resources/geodata-test.json", geoIndex);
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
				client.prepareSearch(CONFIG.getString("index.es.name"))
						.setTypes(CONFIG.getString("index.es.type"))
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.termQuery(aField, aValue)).execute()
						.actionGet();
		return responseOfSearch;
	}

	public static SearchResponse search(final String aField,
			final String aValue) {
		SearchResponse responseOfSearch =
				client.prepareSearch(CONFIG.getString("index.es.name"))
						.setTypes(CONFIG.getString("index.es.type"))
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.matchQuery(aField, aValue)).execute()
						.actionGet();
		return responseOfSearch;
	}

}
