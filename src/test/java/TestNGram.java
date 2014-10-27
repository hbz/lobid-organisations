import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import flow.Enrich;

@SuppressWarnings("javadoc")
public class TestNGram {

	private static Node node = nodeBuilder().local(true).node();
	private static Client client = node.client();

	@BeforeClass
	public static void makeIndex() throws IOException {
		Enrich.main();
		createEmptyIndex();
		indexData();
	}

	@AfterClass
	public static void closeNode() {
		node.close();
	}

	private static void indexData() throws IOException {

		BulkRequestBuilder bulkRequest = client.prepareBulk();

		try (BufferedReader br =
				new BufferedReader(new FileReader(
						"src/main/resources/output/enriched.out.json"))) {
			readData(bulkRequest, br);
		}

		bulkRequest.execute().actionGet();
		client.admin().indices().refresh(new RefreshRequest()).actionGet();

	}

	private static void readData(BulkRequestBuilder bulkRequest, BufferedReader br)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		String line;
		int currentLine = 1;
		String organisationId = null;
		String organisationData = null;

		// First line: index with id, second line: source
		while ((line = br.readLine()) != null) {
			if (currentLine % 2 != 0) {
				JsonNode rootNode = mapper.readValue(line, JsonNode.class);
				JsonNode index = rootNode.get("index");
				organisationId = index.findValue("_id").asText();

			} else {
				organisationData = line;
				bulkRequest.add(client.prepareIndex("organisations", "dbs",
						organisationId).setSource(organisationData));
			}
			currentLine++;
		}
	}

	private static void createEmptyIndex() throws JsonParseException,
			JsonMappingException, IOException {

		deleteIndex();

		String settingsMappings =
				Files.lines(Paths.get("src/main/resources/index-settings.json"))
						.collect(Collectors.joining());

		CreateIndexRequestBuilder cirb =
				client.admin().indices().prepareCreate("organisations");

		cirb.setSource(settingsMappings);
		cirb.execute().actionGet();
	}

	private static void deleteIndex() {
		if (client.admin().indices().prepareExists("organisations").execute()
				.actionGet().isExists()) {
			DeleteIndexRequest deleteIndexRequest =
					new DeleteIndexRequest("organisations");
			client.admin().indices().delete(deleteIndexRequest);
		}
	}

	private static SearchResponse search(String nameToSearch) {
		SearchResponse responseOfSearch =
				client.prepareSearch("organisations").setTypes("dbs")
						.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
						.setQuery(QueryBuilders.matchQuery("name", nameToSearch)).execute()
						.actionGet();
		return responseOfSearch;
	}

	@Test
	public void requestFullTerm() {
		long total = search("Stadtbibliothek").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}

	@Test
	public void requestNGram() {
		long total = search("Stadtbib").getHits().getTotalHits();
		assertEquals("Request should return 1", 1, total);
	}
}
