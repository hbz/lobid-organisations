import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("javadoc")
public class TestNGram {

	private static final String URL_ROOT =
			"http://weywot2.hbz-nrw.de:9200/organisations/_search?";

	@Test
	public void requestFullTerm() throws IOException {
		URL url = new URL(URL_ROOT + "q=name:Hochschulbibliothekszentrum'");
		int total = getTotal(url);
		assertEquals("Request should return 6", 6, total);
	}

	@Test
	public void requestNGram() throws IOException {
		URL url = new URL(URL_ROOT + "q=name:Hochschulbibliothekszen'");
		int total = getTotal(url);
		assertEquals("Request should return results for ngram", 6, total);
	}

	private static int getTotal(URL url) throws IOException, JsonParseException,
			JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readValue(url, JsonNode.class);
		int total = rootNode.get("hits").findValue("total").asInt();
		return total;
	}

}
