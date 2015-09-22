package flow;

import static org.junit.Assert.assertTrue;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestGeoEnrich extends ElasticsearchTest {

	private static SearchResponse searchByAddress(String addressToSearch) {
		SearchResponse responseOfSearch = client
				.prepareSearch(ElasticsearchAuxiliary.ES_INDEX)
				.setTypes(ElasticsearchAuxiliary.ES_TYPE)
				.setSearchType(SearchType.DFS_QUERY_AND_FETCH).setQuery(QueryBuilders
						.matchQuery("location.address.streetAddress", addressToSearch))
				.execute().actionGet();
		return responseOfSearch;
	}

	@Test
	public void requestCoordinates() {
		SearchHit response =
				searchByAddress("Universit��tsstr. 33").getHits().getAt(0);
		System.out.println(response.getSourceAsString());
		assertTrue("Response should contain the field location",
				response.getSourceAsString().contains("geo"));
	}
}
