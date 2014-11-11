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
				client.prepareGet("organisations", "organisation", id).execute()
						.actionGet();
		String source = response.getSourceAsString();
		source = replaceContext(source);
		return source;
	}

	private static String replaceContext(String source) {
		String newSource =
				source.replaceAll("http://data.lobid.org/organisations/context.jsonld",
						"http://schema.org");

		/* For testing with local context */
		// File file = new File("web/conf/context.jsonld");
		// String pathToContext = file.getAbsolutePath();
		// String newSource =
		// source.replaceAll("http://data.lobid.org/organisations/context.jsonld",
		// "file://" + pathToContext);

		return newSource;
	}

	@Test
	public void validateJsonLd() throws JsonParseException, IOException,
			JsonLdError {
		Object sourceAsJson = JsonUtils.fromString(getSource("DE-1a"));
		Object sourceAsRdf = JsonLdProcessor.toRDF(sourceAsJson);
		assertNotNull("Index documents should be parsable as JSON-LD", sourceAsRdf);
	}

}
