import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import flow.Enrich;

@SuppressWarnings("javadoc")
public class TestNGram {

	private static Runtime rt = Runtime.getRuntime();
	private static final String SERVER =
			"http://weywot2.hbz-nrw.de:9200/organisations/";

	private static final String SEARCH_ROOT = SERVER + "_search?";

	@BeforeClass
	public static void makeIndex() throws IOException, InterruptedException {
		Enrich.main();
		removeIndex();
		sendSettings();
		indexData();
	}

	private static void indexData() throws IOException {
		String command =
				"curl -s -XPOST " + SERVER + "_bulk"
						+ " --data-binary @src/main/resources/output/enriched.out.json";
		Process createProcess = Runtime.getRuntime().exec(command);
		printProcessOutput(createProcess);
	}

	private static void sendSettings() throws IOException, InterruptedException {
		Process settingsProcess =
				Runtime.getRuntime().exec(
						"curl -XPUT " + SERVER
								+ " --data-binary @src/main/resources/index-settings.json");
		settingsProcess.waitFor();
		printProcessOutput(settingsProcess);
	}

	private static void removeIndex() throws IOException, InterruptedException {
		Process deleteProcess = rt.exec("curl -XDELETE " + SERVER);
		deleteProcess.waitFor();
		printProcessOutput(deleteProcess);
	}

	private static void printProcessOutput(Process p) {
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String outputLine;
		try {
			outputLine = reader.readLine();
			while (outputLine != null) {
				System.out.println(outputLine);
				outputLine = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void requestFullTerm() throws IOException {
		URL url = new URL(SEARCH_ROOT + "q=name:Hochschulbibliothekszentrum'");
		int total = getTotal(url);
		assertEquals("Request should return 6", 6, total);
	}

	@Test
	public void requestNGram() throws IOException {
		URL url = new URL(SEARCH_ROOT + "q=name:Hochschulbibliothekszen'");
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
