package flow;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestNGram extends ElasticsearchTest {

	private static SearchResponse search(String nameToSearch) {
		SearchResponse responseOfSearch =
				client.prepareSearch("organisations").setTypes("organisation")
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
