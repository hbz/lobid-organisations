package flow;

import java.io.IOException;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.source.FileOpener;
import org.lobid.lodmill.XmlEntitySplitter;

/**
 * @author Simon Ritter (SBRitter)
 * 
 *         For tests: sample data only, no updates
 *
 */
public class EnrichSample {

	private static String SIGEL_DUMP_LOCATION = Constants.MAIN_RESOURCES_PATH + Constants.INPUT_PATH + "sigel.xml";
	private static String SIGEL_TEMP_FILES_LOCATION = Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH;
	private static String DBS_LOCATION = Constants.MAIN_RESOURCES_PATH + Constants.INPUT_PATH + "dbs.csv";
	private static String DUMP_XPATH = "/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;

	/**
	 * @param args
	 *            not used
	 */
	public static void main(String... args) {
		try {
			processSample(Constants.MAIN_RESOURCES_PATH
					// TODO: Constants.TEST_RESOURCES_PATH ?
					+ Constants.OUTPUT_PATH + "enriched.out.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static void processSample(final String aOutputPath) throws IOException {

		// setup Sigel flow
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlEntitySplitter xmlSplitter = new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
				Constants.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH);

		final FileOpener splitFileOpener = new FileOpener();
		final StreamToTriples streamToTriplesSigel = Helpers.createTripleStream(true);
		Sigel.setupSigelMorph(splitFileOpener).setReceiver(streamToTriplesSigel);

		Helpers.setupTripleStreamToWriter(streamToTriplesSigel, aOutputPath);

		// setup DBS flow
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = Helpers.createTripleStream(true);
		StreamToTriples dbsFlow = //
		Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);

		Helpers.setupTripleStreamToWriter(dbsFlow, aOutputPath);

		Sigel.processSigelSource(sourceFileOpener, SIGEL_DUMP_LOCATION);
		Sigel.processSigelTriples(splitFileOpener, SIGEL_TEMP_FILES_LOCATION);
		Dbs.processDbs(openDbs, DBS_LOCATION);
	}
}
