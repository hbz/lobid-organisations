/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package index;

import static org.junit.Assert.assertTrue;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import controllers.Application;

@SuppressWarnings("javadoc")
public class TestGeoEnrich extends ElasticsearchTest {

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
		return searchByAddress(Application.CONFIG.getString("index.es.name"),
				client, Application.CONFIG.getString("index.es.type"),
				SearchType.DFS_QUERY_AND_FETCH, query);
	}

	@Test
	public void requestCoordinates() {
		SearchHit response = searchByAddressInOrganisations("Universitätsstr. 33")
				.getHits().getAt(0);
		assertTrue("Response should contain the field location",
				response.getSourceAsString().contains("location"));
	}
}
