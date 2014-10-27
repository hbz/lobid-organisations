package flow;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.JsonToElasticsearchBulk;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Enrich {

	private static Node node;
	private static Client client;

	/**
	 * @param args Not used
	 * @throws IOException if json file with output cannot be found
	 * @throws JsonMappingException if json mapping of organisation data cannot be
	 *           created
	 * @throws JsonParseException if value cannot be read from json mapper
	 */
	public static void main(String... args) throws JsonParseException,
			JsonMappingException, IOException {
		/* Run both preparatory pipelines standalone for debugging, doc etc. */
		Dbs.main();
		Sigel.main();
		/* Run the actual enrichment pipeline, which includes the previous: */
		process();
		/* Build index from output on elasticsearch */
		indexToElasticSearch();
	}

	private static void process() {
		DirReader openSigel = new DirReader();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigel).setReceiver(streamToTriples1);
		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples2);
		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		continueWith(flow1, wait);
		continueWith(flow2, wait);
		Dbs.processDbs(openDbs);
		Sigel.processSigel(openSigel);
	}

	private static void continueWith(StreamToTriples flow,
			CloseSupressor<Triple> wait) {
		TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // remove Sigel entries w/o DBS link
		Metamorph morph = new Metamorph("src/main/resources/morph-enriched.xml");
		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/output/enriched.out.json");
		JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("inr", "dbs", "organisations");
		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(writer);
	}

	private static void indexToElasticSearch() throws JsonParseException,
			JsonMappingException, IOException {
		node = nodeBuilder().local(false).node();
		Settings clientSettings =
				ImmutableSettings.settingsBuilder()
						.put("cluster.name", "organisation-cluster")
						.put("client.transport.sniff", true).build();

		// Funktioniert das nicht?
		try (TransportClient transportClient = new TransportClient(clientSettings)) {
			client =
					transportClient.addTransportAddress(new InetSocketTransportAddress(
							"weywot2.hbz-nrw.de", 9300));

			createEmptyIndex();
			indexData();
			client.close();
			node.close();
		}
	}

	private static void createEmptyIndex() throws IOException {
		deleteIndex();
		String settingsMappings =
				Files.lines(Paths.get("src/main/resources/index-settings.json"))
						.collect(Collectors.joining());
		CreateIndexRequestBuilder cirb =
				client.admin().indices().prepareCreate("organisations");
		cirb.setSource(settingsMappings);
		cirb.execute().actionGet();
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

	private static void deleteIndex() {
		if (client.admin().indices().prepareExists("organisations").execute()
				.actionGet().isExists()) {
			DeleteIndexRequest deleteIndexRequest =
					new DeleteIndexRequest("organisations");
			client.admin().indices().delete(deleteIndexRequest);
		}
	}
}
