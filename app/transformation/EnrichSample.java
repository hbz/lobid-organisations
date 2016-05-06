package transformation;

import java.io.IOException;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

/**
 * For tests: sample data only, no updates.
 * 
 * @author Simon Ritter (SBRitter)
 */
public class EnrichSample {

	private static String SIGEL_DUMP_LOCATION =
			Enrich.DATA_INPUT_DIR + "sigel.xml";
	private static String SIGEL_TEMP_FILES_LOCATION =
			Enrich.DATA_OUTPUT_DIR;
	private static String DBS_LOCATION = Enrich.DATA_INPUT_DIR + "dbs.csv";
	private static String DUMP_XPATH =
			"/" + Enrich.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Enrich.SIGEL_XPATH;

	/**
	 * @param aOutputPath The path to which the output of transform should go
	 * @throws IOException If dump files cannot be read
	 */
	public static void processSample(final String aOutputPath)
			throws IOException {

		final CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		final TripleSort sortTriples = new TripleSort();
		final TripleRematch rematchTriples = new TripleRematch("isil");

		// Sigel Splitting
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				Enrich.SIGEL_DUMP_TOP_LEVEL_TAG, Enrich.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH,
				Enrich.DATA_OUTPUT_DIR);
		Sigel.processSigelSplitting(sourceFileOpener, SIGEL_DUMP_LOCATION);

		// DBS transformation
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples dbsFlow = //
				Dbs.morphDbs(openDbs).setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(dbsFlow, wait, sortTriples,
				rematchTriples, aOutputPath, null);
		Dbs.processDbs(openDbs, DBS_LOCATION);

		// Sigel Morph
		final FileOpener splitFileOpener = new FileOpener();
		StreamToTriples sigelFlow = Sigel.setupSigelMorph(splitFileOpener)
				.setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(sigelFlow, wait, sortTriples,
				rematchTriples, aOutputPath, null);
		Sigel.processSigelMorph(splitFileOpener, SIGEL_TEMP_FILES_LOCATION);
	}

}
