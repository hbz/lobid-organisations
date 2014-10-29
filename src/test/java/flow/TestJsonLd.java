package flow;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.elasticsearch.action.get.GetResponse;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

@SuppressWarnings("javadoc")
public class TestJsonLd extends ElasticsearchTest {

	private static String getSource(String id) {
		GetResponse response =
				client.prepareGet("organisations", "dbs", id).execute().actionGet();
		return response.getSourceAsString();
	}

	@Test
	public void validateJsonLd() throws JsonParseException, IOException,
			JsonLdError {
		Object sourceAsJson = JsonUtils.fromString(getSource("AB038"));
		Object sourceAsRdf = JsonLdProcessor.toRDF(sourceAsJson);
		assertNotNull("Index documents should be parsable as JSON-LD", sourceAsRdf);
	}

}
