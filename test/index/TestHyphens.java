/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package index;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.search.SearchHits;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestHyphens extends ElasticsearchTest {

	@Test
	public void testResultsForHyphenedWord() throws JSONException {
		SearchHits hits = search("_all", "Roemermuseum").getHits();
		JSONObject firstHit = new JSONObject(hits.getAt(0).sourceAsString());
		assertEquals("Roemer-Museum, Bibliothek", firstHit.get("name"));
	}

}
