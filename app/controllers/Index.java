/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.xbib.elasticsearch.plugin.bundle.BundlePlugin;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import transformation.TransformAll;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Indexing of the transformed data in elasticsearch
 * 
 * Settings and mappings are chosen such as to allow ngram search
 * 
 * @author Simon Ritter (SBRitter), Fabian Steeg (fsteeg)
 *
 */
public class Index extends Controller {

	static final String GEO_FIELD = "location.geo";

	static final String INDEX_NAME =
			Application.CONFIG.getString("index.es.name");
	private static final String INDEX_TYPE =
			Application.CONFIG.getString("index.es.type");

	private static class ConfigurableNode extends Node {
		public ConfigurableNode(Settings settings,
				Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(settings, null),
					Version.CURRENT, classpathPlugins);
		}
	}

	private final static Settings clientSettings =
			Settings.settingsBuilder().put("path.home", ".")
					.put("http.port", Application.CONFIG.getString("index.es.port.http"))
					.put("transport.tcp.port",
							Application.CONFIG.getString("index.es.port.tcp"))
					.put("script.default_lang", "native").build();

	private final static Node node = new ConfigurableNode(
			nodeBuilder().settings(clientSettings).local(true).getSettings().build(),
			Arrays.asList(BundlePlugin.class, LocationAggregation.class, Zero.class))
					.start();
	/**
	 * The Elasticsearch client to be used by all parts of the application
	 */
	public static final Client CLIENT = node.client();

	static final List<String> SUPPORTED_AGGREGATIONS =
			Arrays.asList("type.raw", "classification.label.de.raw",
					"classification.label.en.raw", "fundertype.label.de.raw",
					"fundertype.label.en.raw", "collects.extent.label.de.raw",
					"collects.extent.label.en.raw", "location.raw");

	/**
	 * @throws IOException if json file with output cannot be found
	 * @return 200 ok or 403 forbidden response depending on ip address of client
	 */
	public static Result start() throws IOException {
		String remote = request().remoteAddress();
		if (!Application.CONFIG.getStringList("index.remote").contains(remote)) {
			return forbidden();
		}
		initialize(TransformAll.DATA_OUTPUT_FILE);
		return ok("Started indexing");
	}

	/**
	 * @param pathToJson Path to the JSON file to index
	 * @throws IOException              if json file cannot be found
	 * @throws IllegalArgumentException if to be indexed file is smaller than expected
	 * @throws ElasticsearchException   if bulk indexing fails
	 */
	public static void initialize(String pathToJson) throws IOException, ElasticsearchException {
		long minimumSize =
				Long.parseLong(Application.CONFIG.getString("index.file.minsize"));
		if (new File(pathToJson).length() >= minimumSize) {
			createEmptyIndex();
			indexData(CLIENT, pathToJson, INDEX_NAME);
		}
		else {
			throw new IllegalArgumentException(
					"File " + pathToJson + " is not large enough - should be >='" + minimumSize + "' but is " + pathToJson.length());
		}
	}

	/** Close the embedded Elasticsearch index. */
	public static void close() {
		node.close();
	}

	/**
	 * @param id The document ID
	 * @return The document with the given ID, or null, if it does not exist
	 */
	public static String get(String id) {
		GetResponse response =
				CLIENT.prepareGet("organisations", "organisation", id).get();
		return response.isExists() ? response.getSourceAsString() : null;
	}

	static SearchResponse executeQuery(int from, int size, QueryBuilder query,
			String aggregations) {
		SearchRequestBuilder searchRequest = Index.CLIENT.prepareSearch(INDEX_NAME)
				.setTypes(INDEX_TYPE).setSearchType(SearchType.QUERY_THEN_FETCH)
				.setQuery(preprocess(query)).setFrom(from).setSize(size);
		if (!aggregations.isEmpty()) {
			searchRequest = withAggregations(searchRequest, aggregations.split(","));
		}
		return searchRequest.execute().actionGet();
	}

	private static QueryBuilder preprocess(QueryBuilder query) {
		String position = session("position");
		if (position != null) {
			Logger.info("Sorting by distance to current position {}", position);
			ScoreFunctionBuilder locationScore = ScoreFunctionBuilders
					.linearDecayFunction(GEO_FIELD, new GeoPoint(position), "3km")
					.setOffset("0km");
			return QueryBuilders.functionScoreQuery(query).boostMode("sum")
					.add(QueryBuilders.existsQuery(GEO_FIELD), locationScore)
					.add(ScoreFunctionBuilders.scriptFunction(new Script("zero")))
					.scoreMode("first");
		}
		return query;
	}

	private static SearchRequestBuilder withAggregations(
			final SearchRequestBuilder searchRequest, String... fields) {
		Arrays.asList(fields).forEach(field -> {
			if (field.startsWith("location")) {
				TopHitsBuilder topHitsBuilder = AggregationBuilders.topHits(GEO_FIELD)
						.addScriptField("pin", new Script("location-aggregation"))
						.setSize(Integer.MAX_VALUE);
				String position = session("position");
				if (position != null) {
					topHitsBuilder.setSize(Integer.MAX_VALUE)
							.addSort(new GeoDistanceSortBuilder(GEO_FIELD)
									.points(new GeoPoint(position)));
				}
				searchRequest.addAggregation(topHitsBuilder);
			} else {
				searchRequest
						.addAggregation(AggregationBuilders.terms(field.replace(".raw", ""))
								.field(field).size(Integer.MAX_VALUE));
			}
		});
		return searchRequest;
	}

	static void createEmptyIndex() throws IOException {
		deleteIndex(Index.CLIENT, Index.INDEX_NAME);
		CreateIndexRequestBuilder cirb =
				Index.CLIENT.admin().indices().prepareCreate(Index.INDEX_NAME);
		String settingsMappings = Files.lines(Paths.get("conf/index-settings.json"))
				.collect(Collectors.joining());
		cirb.setSource(settingsMappings);
		cirb.execute().actionGet();
		Index.CLIENT.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	static void indexData(final Client aClient, final String aPath, final String aIndex) throws IOException, ElasticsearchException {
		final BulkRequestBuilder bulkRequest = Index.CLIENT.prepareBulk();
		try (BufferedReader br =
				new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(aPath)),
						StandardCharsets.UTF_8))) {
			readData(bulkRequest, br, aClient, aIndex);
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			throw new ElasticsearchException("Bulk insert failed: " + bulkResponse.buildFailureMessage());
		}
		aClient.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	private static void readData(final BulkRequestBuilder bulkRequest,
			final BufferedReader br, final Client client, final String aIndex)
			throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		String line;
		int currentLine = 1;
		String organisationData;
		String[] idUriParts;
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
				bulkRequest.add(Index.CLIENT.prepareIndex(Index.INDEX_NAME, INDEX_TYPE, organisationId)
						.setSource(organisationData));
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
