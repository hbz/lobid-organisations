package flow;

import static org.junit.Assert.assertTrue;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestGeoEnrich extends ElasticsearchTest {

	private static final String GEO_INDEX_TEST = "geodata";

	private static SearchResponse searchByAddress(final String aIndex,
			final Client aClient, final String aType, SearchType aSearchType,
			QueryBuilder aQuery) {
		SearchRequestBuilder searchRequest = aClient.prepareSearch(aIndex);
		if (aType != null) {
			searchRequest.setTypes(aType);
		}
		if (aSearchType != null) {
			searchRequest.setSearchType(aSearchType);
		}
		searchRequest.setQuery(aQuery);
		return searchRequest.execute().actionGet();
	}

	private static SearchResponse searchByAddressInOrganisations(
			final String addressToSearch) {
		MatchQueryBuilder query = QueryBuilders
				.matchQuery("location.address.streetAddress", addressToSearch);
		return searchByAddress(Constants.ES_INDEX, client, Constants.ES_TYPE,
				SearchType.DFS_QUERY_AND_FETCH, query);
	}

	private static SearchResponse searchByAddressInGeodata() {
		QueryStringQueryBuilder query =
				QueryBuilders.queryStringQuery("Grabenstr. 4");
		return searchByAddress(GEO_INDEX_TEST, geoClient, null, null, query);
	}

	@Test
	public void requestCoordinates() {
		SearchHit response = searchByAddressInOrganisations("Universitätsstr. 33")
				.getHits().getAt(0);
		assertTrue("Response should contain the field location",
				response.getSourceAsString().contains("geo"));
	}

	@Test
	public void testDbsGeoEnrichment() throws InterruptedException {
		// wait for Geo Index to be set up
		int count = 0;
		while (!geoClient.admin().indices().prepareExists(GEO_INDEX_TEST).execute()
				.actionGet().isExists()) {
			Thread.sleep(1000);
			count++;
			if (count > 30) {
				break;
			}
		}
		SearchHit responseGeoIndex = searchByAddressInGeodata().getHits().getAt(0);
		assertTrue("Response should contain the geo location field",
				responseGeoIndex.getSourceAsString().contains("geocode"));
		// TODO: test actual content of "geocode" field
	}

}
