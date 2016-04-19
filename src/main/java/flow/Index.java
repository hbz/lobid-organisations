package flow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

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
	public static void main(final String... args)
			throws JsonParseException, JsonMappingException, IOException {
		long minimumSize = Long.parseLong(args[0]);
		String pathToJson = args[1];
		String index = args.length == 3 ? args[2] : Constants.ES_INDEX;
		if (new File(pathToJson).length() >= minimumSize) {
			Settings clientSettings =
					Settings.settingsBuilder().put("cluster.name", Constants.ES_CLUSTER)
							.put("client.transport.sniff", true).build();
			try (
					TransportClient transportClient =
							TransportClient.builder().settings(clientSettings).build();
					Client client = transportClient
							.addTransportAddress(new InetSocketTransportAddress(
									new InetSocketAddress(Constants.SERVER_NAME, 9300)));) {
				createEmptyIndex(client, Constants.ES_INDEX,
						Constants.MAIN_RESOURCES_PATH + "index-settings.json");
				indexData(client, pathToJson, index);
				client.close();
			}
		} else {
			throw new IllegalArgumentException(
					"File not large enough: " + pathToJson);
		}
	}

	static void createEmptyIndex(final Client aClient, final String aIndexName,
			final String aPathToIndexSettings) throws IOException {
		deleteIndex(aClient, aIndexName);
		CreateIndexRequestBuilder cirb =
				aClient.admin().indices().prepareCreate(aIndexName);
		if (aPathToIndexSettings != null) {
			String settingsMappings = Files.lines(Paths.get(aPathToIndexSettings))
					.collect(Collectors.joining());
			cirb.setSource(settingsMappings);
		}
		cirb.execute().actionGet();
		aClient.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	static void indexData(final Client aClient, final String aPath,
			final String aIndex) throws IOException {
		final BulkRequestBuilder bulkRequest = aClient.prepareBulk();
		try (BufferedReader br =
				new BufferedReader(new InputStreamReader(new FileInputStream(aPath),
						StandardCharsets.UTF_8))) {
			readData(bulkRequest, br, aClient, aIndex);
		}
		bulkRequest.execute().actionGet();
		aClient.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	private static void readData(final BulkRequestBuilder bulkRequest,
			final BufferedReader br, final Client client, final String aIndex)
					throws IOException, JsonParseException, JsonMappingException {
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
				bulkRequest
						.add(client.prepareIndex(aIndex, Constants.ES_TYPE, organisationId)
								.setSource(organisationData));
			}
			currentLine++;
		}
	}

	private static void deleteIndex(final Client client, final String index) {
		if (client.admin().indices().prepareExists(index).execute().actionGet()
				.isExists()) {
			final DeleteIndexRequest deleteIndexRequest =
					new DeleteIndexRequest(index);
			client.admin().indices().delete(deleteIndexRequest);
			client.admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();
		}
	}
}
