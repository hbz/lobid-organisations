package flow;

import java.io.IOException;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

/**
 * @author Simon Ritter (SBRitter)
 * 
 *         For tests: sample data only, no updates
 *
 */
public class EnrichSample {

	private static String SIGEL_DUMP_LOCATION =
			Constants.TEST_RESOURCES_PATH + Constants.INPUT_PATH + "sigel.xml";
	private static String SIGEL_TEMP_FILES_LOCATION =
			Constants.TEST_RESOURCES_PATH + Constants.OUTPUT_PATH;
	private static String DBS_LOCATION =
			Constants.TEST_RESOURCES_PATH + Constants.INPUT_PATH + "dbs.csv";
	private static String DUMP_XPATH =
			"/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;

	private static TripleSort tripleSort = new TripleSort();

	/**
	 * @param args not used
	 */
	public static void main(String... args) {
		try {
			processSample(Constants.TEST_RESOURCES_PATH + Constants.OUTPUT_PATH
					+ "enriched.out.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void processSample(final String aOutputPath) throws IOException {

		final CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		final TripleSort sortTriples = new TripleSort();
		final TripleRematch rematchTriples = new TripleRematch("isil");

		// Sigel Splitting
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				Constants.SIGEL_DUMP_TOP_LEVEL_TAG, Constants.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH,
				Constants.TEST_RESOURCES_PATH + Constants.OUTPUT_PATH);
		Sigel.processSigelSplitting(sourceFileOpener, SIGEL_DUMP_LOCATION);

		// DBS flow
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples dbsFlow = //
				Dbs.morphDbs(openDbs).setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(dbsFlow, wait, sortTriples,
				rematchTriples, aOutputPath);
		Dbs.processDbs(openDbs, DBS_LOCATION);

		// Sigel Morph
		final FileOpener splitFileOpener = new FileOpener();
		StreamToTriples sigelFlow = Sigel.setupSigelMorph(splitFileOpener)
				.setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(sigelFlow, wait, sortTriples,
				rematchTriples, aOutputPath);
		Sigel.processSigelMorph(splitFileOpener, SIGEL_TEMP_FILES_LOCATION);
	}

}
