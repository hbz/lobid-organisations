/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package index;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import controllers.Application;
import controllers.Index;
import transformation.TransformAll;

@SuppressWarnings("javadoc")
public abstract class ElasticsearchTest {

	static {
		System.setProperty("config.resource", "test.conf");
		System.out.println("Using CONFIG from " + Application.CONFIG.origin());
	}

	protected static Client client = Index.CLIENT;

	@BeforeClass
	public static void makeIndex() throws IOException {
		Index.initialize(TransformAll.DATA_OUTPUT_FILE);
	}

	@AfterClass
	public static void closeElasticSearch() {
		client.close();
	}

	public static SearchResponse search(final String aField,
			final String aValue) {
		SearchResponse responseOfSearch =
				client.prepareSearch(Application.CONFIG.getString("index.es.name"))
						.setTypes(Application.CONFIG.getString("index.es.type"))
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.matchQuery(aField, aValue)).execute()
						.actionGet();
		return responseOfSearch;
	}

}
