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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.mvc.Controller;
import play.mvc.Result;
import transformation.Enrich;

/**
 * Indexing of the transformed data in elasticsearch
 * 
 * Settings and mappings are chosen such as to allow ngram search
 * 
 * @author Simon Ritter (SBRitter), Fabian Steeg (fsteeg)
 *
 */
public class Index extends Controller {

	private static class ConfigurableNode extends Node {
		public ConfigurableNode(Settings settings,
				Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(settings, null),
					Version.CURRENT, classpathPlugins);
		}
	}

	static Settings clientSettings =
			Settings.settingsBuilder().put("path.home", ".")
					.put("http.port", Application.CONFIG.getString("index.es.port.http"))
					.put("transport.tcp.port",
							Application.CONFIG.getString("index.es.port.tcp"))
					.put("script.default_lang", "native").build();
	private static Node node = new ConfigurableNode(
			nodeBuilder().settings(clientSettings).local(true).getSettings().build(),
			Arrays.asList(LocationAggregation.class)).start();
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
		if (!Application.CONFIG.getStringList("index.remote").contains(remote)) {
			return forbidden();
		}
		initialize(Enrich.DATA_OUTPUT_FILE);
		return ok("Started indexing");
	}

	/**
	 * @param pathToJson Path to the JSON file to index
	 * @throws IOException if json file cannot be found
	 */
	public static void initialize(String pathToJson) throws IOException {
		long minimumSize =
				Long.parseLong(Application.CONFIG.getString("index.file.minsize"));
		String index = Application.CONFIG.getString("index.es.name");
		if (new File(pathToJson).length() >= minimumSize) {
			createEmptyIndex(CLIENT, index, "conf/index-settings.json");
			indexData(CLIENT, pathToJson, index);
		} else {
			throw new IllegalArgumentException(
					"File not large enough: " + pathToJson);
		}
	}

	/** Close the embedded Elasticsearch index. */
	public static void close() {
		node.close();
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
			throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		String line;
		int currentLine = 1;
		String organisationData = null;
		String[] idUriParts = null;
		String organisationId = null;

		// First line: index with id, second line: source
		while ((line = br.readLine()) != null) {
			JsonNode rootNode = mapper.readValue(line, JsonNode.class);
			if (currentLine % 2 != 0) {
				JsonNode index = rootNode.get("index");
				idUriParts = index.findValue("_id").asText().split("/");
				organisationId = idUriParts[idUriParts.length - 1].replace("#!", "");
			} else {
				organisationData = line;
				JsonNode libType = rootNode.get("type");
				if (libType == null || !libType.textValue().equals("Collection")) {
					bulkRequest.add(client
							.prepareIndex(aIndex,
									Application.CONFIG.getString("index.es.type"), organisationId)
							.setSource(organisationData));
				}
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
