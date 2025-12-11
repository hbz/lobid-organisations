/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.metafacture.metafix.Metafix;
import org.metafacture.elasticsearch.JsonToElasticsearchBulk;

import controllers.Application;
import transformation.GeoLookupMap.LookupType;

/**
 * Transform DBS and Sigel data to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg), Simon Ritter (SBRitter)
 *
 */
public class TransformAll {

	/** The output directory. If not set in config, system temp is used. */
	public static final String DATA_OUTPUT_DIR =
			Application.CONFIG.hasPath("data.output.dir")
					? Application.CONFIG.getString("data.output.dir")
					: System.getProperty("java.io.tmpdir") + "/lobid-organisations";

	static {
		new File(DATA_OUTPUT_DIR).mkdir();
	}

	/** The output file path. */
	public static final String DATA_OUTPUT_FILE = DATA_OUTPUT_DIR + "/"
			+ controllers.Application.CONFIG.getString("index.file.name");

	static final String DATA_INPUT_DIR =
			Application.CONFIG.getString("data.input.dir");

	/**
	 * @param startOfUpdates Date from which updates should start
	 * @param outputPath The path to which the output of transform should go
	 * @param geoServer The lookup server for geo data
	 * @throws IOException If dump and temp files cannot be read
	 */
	public static void process(final String startOfUpdates,
			String outputPath, final String geoServer, final String wikidataLookupFilename) throws IOException {
		String dbsOutput = outputPath + "-dbs";
		String sigelBulkOutput = outputPath + "-sigelBulk";
		String sigelUpdatesOutput = outputPath + "-sigelUpdates";
		TransformSigel.processBulk(sigelBulkOutput, geoServer, wikidataLookupFilename); //Start processing  Sigel pica binary bulk.
		if (startOfUpdates != "") { // exclude updates for the tests, which set startOfUpdates to ""
			TransformSigel.processUpdates(startOfUpdates, sigelUpdatesOutput, geoServer, wikidataLookupFilename); //Start process Sigel Pica XML Updates via OAI-PMH.
			}
		TransformDbs.process(dbsOutput, geoServer,wikidataLookupFilename); //Start process DBS CSV data.
		
		// DBS-Data, Sigel Bulk and Updates are joined in a single ES-Bulk-file.
		// DBS data first, so that ES prefers Sigel entries that come later and overwrite DBS entries if available.
		try (FileWriter resultWriter = new FileWriter(outputPath)) {
			writeAll(dbsOutput, resultWriter);
			writeAll(sigelBulkOutput, resultWriter);
			if (startOfUpdates != "") { // exclude updates for the tests, which set startOfUpdates to ""
				writeAll(sigelUpdatesOutput, resultWriter);
			}
		}
	}

	private static void writeAll(String output, FileWriter resultWriter)
			throws IOException {
		Files.readAllLines(Paths.get(output)).forEach(line -> {
			try {
				resultWriter.write(line);
				resultWriter.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	static JsonToElasticsearchBulk esBulk() {
		return new JsonToElasticsearchBulk("id",
				Application.CONFIG.getString("index.es.type"),
				Application.CONFIG.getString("index.es.name"));
	}

	static Metafix fixEnriched(final String geoLookupServer, final String wikidataLookupFilename) throws IOException, FileNotFoundException {
		final HashMap<String, String> fixVariables = new HashMap<>();
		fixVariables.put("isil2wikidata", wikidataLookupFilename);
		fixVariables.put("dbsID2wikidata", wikidataLookupFilename);
		fixVariables.put("wikidata2gndIdentifier", wikidataLookupFilename);
		Metafix fixEnriched = new Metafix("conf/fix-enriched.fix", fixVariables);

		if (geoLookupServer != null && !geoLookupServer.isEmpty()) {
			fixEnriched.putMap("addLatMap", new GeoLookupMap(LookupType.LAT));
			fixEnriched.putMap("addLongMap", new GeoLookupMap(LookupType.LON));
		}
		return fixEnriched;
	}

}
