package flow;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestNGram {

	private static Node node = nodeBuilder().local(true).node();
	private static Client client = node.client();

	@BeforeClass
	public static void makeIndex() throws IOException {
		/* Data transformation */
		Dbs.main();
		Sigel.main();
		Enrich.process();

		/* Index creation */
		Index.createEmptyIndex(client);
		Index.indexData(client);
	}

	private static SearchResponse search(String nameToSearch) {
		SearchResponse responseOfSearch =
				client.prepareSearch("organisations").setTypes("dbs")
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.matchQuery("name", nameToSearch)).execute()
						.actionGet();
		return responseOfSearch;
	}

	@Test
	public void requestFullTerm() {
		long total = search("Stadtbibliothek").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}

	@Test
	public void requestNGram() {
		long total = search("Stadtbib").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}

	@Test
	public void requestLowerCase() {
		long total = search("stadtbibliothek").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}

	@Test
	public void requestUpperCase() {
		long total = search("STADTBIBLIOTHEK").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}
}
