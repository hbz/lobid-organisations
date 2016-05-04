package controllers;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import play.mvc.Controller;
import play.mvc.Result;

/**
 * Indexing of the transformed data in elasticsearch
 * 
 * Settings and mappings are chosen such as to allow ngram search
 * 
 * @author Simon Ritter (SBRitter)
 *
 */
public class Index extends Controller {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();
	static Settings clientSettings = Settings.settingsBuilder()
			.put("path.home", ".")
			.put("http.port", CONFIG.getString("index.es.port.http"))
			.put("transport.tcp.port", CONFIG.getString("index.es.port.tcp")).build();
	private static Node node =
			nodeBuilder().settings(clientSettings).local(true).node();
	/**
	 * The Elasticsearch client to be used by all parts of the application
	 */
	public static final Client CLIENT = node.client();

	/**
	 * @throws IOException if json file with output cannot be found
	 * @return 200 ok or 403 forbidden response depending on ip address of client
	 */
	public static Result start() throws IOException {
		String remote = request().remoteAddress();
		if (!CONFIG.getStringList("index.remote").contains(remote)) {
			return forbidden();
		}
		initializeIndex();
		return ok("Started indexing");
	}

	/**
	 * @throws IOException if json file cannot be found
	 */
	public static void initializeIndex() throws IOException {
		long minimumSize = Long.parseLong(CONFIG.getString("index.file.minsize"));
		String pathToJson = CONFIG.getString("index.file.path");
		String index = CONFIG.getString("index.es.name");
		if (new File(pathToJson).length() >= minimumSize) {
			createEmptyIndex(CLIENT, index, "conf/index-settings.json");
			indexData(CLIENT, pathToJson, index);
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
						.add(client.prepareIndex(aIndex, CONFIG.getString("index.es.type"),
								organisationId).setSource(organisationData));
			}
			currentLine++;
		}
	}

	private static void deleteIndex(final Client client, final String index) {
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute()
				.actionGet();
		if (client.admin().indices().prepareExists(index).execute().actionGet()
				.isExists()) {
			client.admin().indices().delete(new DeleteIndexRequest(index))
					.actionGet();
		}
	}
}
