package flow;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestGeoSearch extends ElasticsearchTest {

	private static SearchResponse geoSearch(double lon, double lat) {
		GeoPolygonFilterBuilder geoFilter =
				FilterBuilders.geoPolygonFilter("location").addPoint(lat - 1, lon - 1)
						.addPoint(lat - 1, lon + 1).addPoint(lat + 1, lon - 1)
						.addPoint(lat + 1, lon + 1);
		FilteredQueryBuilder geoQuery =
				QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), geoFilter);
		SearchResponse responseOfSearch =
				client.prepareSearch("organisations").setTypes("dbs")
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH).setQuery(geoQuery)
						.execute().actionGet();
		return responseOfSearch;
	}

	@Test
	public void test() {
		SearchResponse response = geoSearch(13, 52);
		long hits = response.getHits().getTotalHits();
		assertEquals("Request should return 1 hit", 1, hits);
	}

}
