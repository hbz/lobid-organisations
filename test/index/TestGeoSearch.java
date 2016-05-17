package index;

import static org.elasticsearch.index.query.QueryBuilders.geoPolygonQuery;
import static org.junit.Assert.assertEquals;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

import controllers.Application;

@SuppressWarnings("javadoc")
public class TestGeoSearch extends ElasticsearchTest {

	private static SearchResponse geoSearch(double lon, double lat) {
		QueryBuilder geoQuery = geoPolygonQuery("location.geo")
				.addPoint(lat - 1, lon - 1).addPoint(lat - 1, lon + 1)
				.addPoint(lat + 1, lon - 1).addPoint(lat + 1, lon + 1);
		SearchResponse responseOfSearch =
				client.prepareSearch(Application.CONFIG.getString("index.es.name"))
						.setTypes(Application.CONFIG.getString("index.es.type"))
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH).setQuery(geoQuery)
						.execute().actionGet();
		return responseOfSearch;
	}

	@Test
	public void requestGeo() {
		SearchResponse response = geoSearch(13, 52);
		long hits = response.getHits().getTotalHits();
		assertEquals("Request should return 1 hit", 1, hits);
	}
}
