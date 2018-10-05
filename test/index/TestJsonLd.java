/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import controllers.Application;

@SuppressWarnings("javadoc")
public class TestJsonLd extends ElasticsearchTest {

	private static String getSource(String id) {
		GetResponse response = client
				.prepareGet(Application.CONFIG.getString("index.es.name"),
						Application.CONFIG.getString("index.es.type"), id)
				.execute().actionGet();
		String source = response.getSourceAsString();
		return source;
	}

	@Test
	public void validateJsonLd()
			throws JsonParseException, IOException, JsonLdError {
		Object sourceAsJson = JsonUtils.fromString(getSource("DE-1a"));
		Object sourceAsRdf = JsonLdProcessor.toRDF(sourceAsJson);
		assertNotNull("Index documents should be parsable as JSON-LD", sourceAsRdf);
	}

	@Test
	public void testOverwrite() throws JsonParseException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> entries =
				((HashMap<String, Object>) JsonUtils.fromString(getSource("DE-38")));
		String name = entries.get("name").toString();
		assertFalse(name.contains("Stattbibliothek"));
		assertTrue(name.contains("Stadtbibliothek"));
	}

}
