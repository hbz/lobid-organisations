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

	/**
	 * @param args Minimum size of json file to be indexed (in bytes)
	 * @throws IOException if json file with output cannot be found
	 * @throws JsonMappingException if json mapping of organisation data cannot be
	 *           created
	 * @throws JsonParseException if value cannot be read from json mapper
	 */
	public static void main(final String... args) throws JsonParseException,
			JsonMappingException, IOException {
		long minimumSize = Long.parseLong(args[0]);
		String aPathToJson = args[1];
		if (new File(aPathToJson).length() >= minimumSize) {
			Settings clientSettings =
					ImmutableSettings.settingsBuilder()
							.put("cluster.name", ElasticsearchAuxiliary.ES_CLUSTER)
							.put("client.transport.sniff", true).build();
			try (Node node = NodeBuilder.nodeBuilder().local(false).node();
					TransportClient transportClient = new TransportClient(clientSettings);
					Client client =
							transportClient
									.addTransportAddress(new InetSocketTransportAddress(
											ElasticsearchAuxiliary.SERVER_NAME, 9300));) {
				createEmptyIndex(client);
				indexData(client, aPathToJson);
				client.close();
				node.close();
			}
		} else {
			throw new IllegalArgumentException("File not large enough: "
					+ aPathToJson);
		}
	}

	static void createEmptyIndex(final Client client) throws IOException {
		deleteIndex(client);
		String settingsMappings =
				Files.lines(
						Paths.get(ElasticsearchAuxiliary.MAIN_RESOURCES_PATH
								+ "index-settings.json")).collect(Collectors.joining());
		CreateIndexRequestBuilder cirb =
				client.admin().indices().prepareCreate(ElasticsearchAuxiliary.ES_INDEX);
		cirb.setSource(settingsMappings);
		cirb.execute().actionGet();
		client.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	static void indexData(final Client aClient, final String aPath)
			throws IOException {
		final BulkRequestBuilder bulkRequest = aClient.prepareBulk();
		try (BufferedReader br = new BufferedReader(new FileReader(aPath))) {
			readData(bulkRequest, br, aClient);
		}
		bulkRequest.execute().actionGet();
		aClient.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	private static void readData(final BulkRequestBuilder bulkRequest,
			final BufferedReader br, final Client client) throws IOException,
			JsonParseException, JsonMappingException {
		final ObjectMapper mapper = new ObjectMapper();
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
				organisationId = idUriParts[idUriParts.length - 1].replace("#!", "");
			} else {
				organisationData = line;
				bulkRequest.add(client.prepareIndex(ElasticsearchAuxiliary.ES_INDEX,
						ElasticsearchAuxiliary.ES_TYPE, organisationId).setSource(
						organisationData));
			}
			currentLine++;
		}
	}

	private static void deleteIndex(final Client client) {
		if (client.admin().indices().prepareExists(ElasticsearchAuxiliary.ES_INDEX)
				.execute().actionGet().isExists()) {
			final DeleteIndexRequest deleteIndexRequest =
					new DeleteIndexRequest(ElasticsearchAuxiliary.ES_INDEX);
			client.admin().indices().delete(deleteIndexRequest);
		}
	}
}
