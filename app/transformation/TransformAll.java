/* Copyright 2014-2017, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonToElasticsearchBulk;

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
	 * @param intervalSize Days to load update for at once
	 * @param outputPath The path to which the output of transform should go
	 * @param geoServer The lookup server for geo data
	 * @throws IOException If dump and temp files cannot be read
	 */
	public static void process(String startOfUpdates, int intervalSize,
			final String outputPath, String geoServer) throws IOException {
		String dbsOutput = outputPath + "-dbs";
		String sigelOutput = outputPath + "-sigel";
		TransformSigel.process(startOfUpdates, intervalSize, sigelOutput,
				geoServer);
		TransformDbs.process(dbsOutput, geoServer);
		try (FileWriter resultWriter = new FileWriter(outputPath)) {
			writeAll(dbsOutput, resultWriter);
			writeAll(sigelOutput, resultWriter);
		}
	}

	private static void writeAll(String dbsOutput, FileWriter resultWriter)
			throws IOException {
		Files.readAllLines(Paths.get(dbsOutput)).forEach(line -> {
			try {
				resultWriter.write(line);
				resultWriter.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	static JsonToElasticsearchBulk esBulk() {
		final JsonToElasticsearchBulk esBulk = new JsonToElasticsearchBulk("id",
				Application.CONFIG.getString("index.es.type"),
				Application.CONFIG.getString("index.es.name"));
		return esBulk;
	}

	static Metamorph morphEnriched(String geoLookupServer) {
		final Metamorph morphEnriched = new Metamorph("morph-enriched.xml");
		if (geoLookupServer != null && !geoLookupServer.isEmpty()) {
			morphEnriched.putMap("addLatMap", new GeoLookupMap(LookupType.LAT));
			morphEnriched.putMap("addLongMap", new GeoLookupMap(LookupType.LON));
		}
		return morphEnriched;
	}

}
