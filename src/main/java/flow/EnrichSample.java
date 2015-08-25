package flow;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;
import org.lobid.lodmill.XmlEntitySplitter;

/**
 * @author Simon Ritter (SBRitter)
 * 
 *         For tests: sample data only, no updates
 *
 */
public class EnrichSample {

	private static String SIGEL_DUMP_LOCATION = Constants.MAIN_RESOURCES_PATH
			+ Constants.INPUT_PATH + "sigel.xml";
	private static String DBS_LOCATION = Constants.MAIN_RESOURCES_PATH
			+ Constants.INPUT_PATH + "dbs.csv";
	private static String DUMP_XPATH = "/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG
			+ "/" + Constants.SIGEL_XPATH;

	/**
	 * @param args not used
	 */
	public static void main(String... args) {
		processSample(Constants.MAIN_RESOURCES_PATH
		// TODO: Constants.TEST_RESOURCES_PATH ?
				+ Constants.OUTPUT_PATH + "enriched.out.json");
	}

	static void processSample(final String aOutputPath) {

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);

		// setup Sigel flow
		final FileOpener openSigelDump = new FileOpener();
		final StreamToTriples streamToTriplesSigel =
				Helpers.createTripleStream(true);
		final XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		final StreamToTriples sigelFlow = //
				Sigel.setupSigelMorph(openSigelDump, xmlSplitter, DUMP_XPATH)
						.setReceiver(streamToTriplesSigel);

		// setup DBS flow
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = Helpers.createTripleStream(true);
		StreamToTriples dbsFlow = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);

		Enrich.setupTripleStreamToWriter(sigelFlow, wait, aOutputPath);
		Enrich.setupTripleStreamToWriter(dbsFlow, wait, aOutputPath);

		Sigel.processSigel(openSigelDump, SIGEL_DUMP_LOCATION);
		Dbs.processDbs(openDbs, DBS_LOCATION);
	}
}
