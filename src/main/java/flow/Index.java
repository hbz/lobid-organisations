package flow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Indexing of the transformed data in elasticsearch
 * 
 * Settings and mappings are chosen such as to allow ngram search
 * 
 * @author Simon Ritter (SBRitter)
 *
 */
public class Index {

	private static final String ORGANISATION = "organisation";
	private static final String ORGANISATIONS = "organisations";

	/**
	 * @param args Minimum size of json file to be indexed (in bytes)
	 * @throws IOException if json file with output cannot be found
	 * @throws JsonMappingException if json mapping of organisation data cannot be
	 *           created
	 * @throws JsonParseException if value cannot be read from json mapper
	 */
	public static void main(String... args) throws JsonParseException,
			JsonMappingException, IOException {
		long minimumSize = Long.parseLong(args[0]);
		if (checkFileSize() >= minimumSize) {
			Settings clientSettings =
					ImmutableSettings.settingsBuilder()
							.put("cluster.name", "elasticsearch")
							.put("client.transport.sniff", true).build();
			try (Node node = NodeBuilder.nodeBuilder().local(false).node();
					TransportClient transportClient = new TransportClient(clientSettings);
					Client client =
							transportClient
									.addTransportAddress(new InetSocketTransportAddress(
											"localhost", 9300));) {
				createEmptyIndex(client);
				indexData(client);
				client.close();
				node.close();
			}
		} else {
			System.out.println("File not large enough.");
		}
	}

	private static long checkFileSize() {
		File enrichedData = new File("src/main/resources/output/enriched.out.json");
		long enrichedLength = enrichedData.length();
		return enrichedLength;
	}

	static void createEmptyIndex(Client client) throws IOException {
		deleteIndex(client);
		String settingsMappings =
				Files.lines(Paths.get("src/main/resources/index-settings.json"))
						.collect(Collectors.joining());
		CreateIndexRequestBuilder cirb =
				client.admin().indices().prepareCreate(ORGANISATIONS);
		cirb.setSource(settingsMappings);
		cirb.execute().actionGet();
	}

	static void indexData(Client client) throws IOException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		try (BufferedReader br =
				new BufferedReader(new FileReader(
						"src/main/resources/output/enriched.out.json"))) {
			readData(bulkRequest, br, client);
		}
		bulkRequest.execute().actionGet();
		client.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	private static void readData(BulkRequestBuilder bulkRequest,
			BufferedReader br, Client client) throws IOException, JsonParseException,
			JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		String line;
		int currentLine = 1;
		String organisationData = null;
		String[] idUriParts = null;
		String organisationId = null;

		// First line: index with id, second line: source
		while ((line = br.readLine()) != null) {
			if (currentLine % 2 != 0) {
				JsonNode rootNode = mapper.readValue(line, JsonNode.class);
				JsonNode index = rootNode.get("index");
				idUriParts = index.findValue("_id").asText().split("/");
				organisationId = idUriParts[idUriParts.length - 1];
			} else {
				organisationData = line;
				bulkRequest.add(client.prepareIndex(ORGANISATIONS, ORGANISATION,
						organisationId).setSource(organisationData));
			}
			currentLine++;
		}
	}

	private static void deleteIndex(Client client) {
		if (client.admin().indices().prepareExists(ORGANISATIONS).execute()
				.actionGet().isExists()) {
			DeleteIndexRequest deleteIndexRequest =
					new DeleteIndexRequest(ORGANISATIONS);
			client.admin().indices().delete(deleteIndexRequest);
		}
	}
}
