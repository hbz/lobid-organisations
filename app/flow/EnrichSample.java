package flow;

import java.io.File;
import java.io.IOException;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author Simon Ritter (SBRitter)
 * 
 *         For tests: sample data only, no updates
 *
 */
public class EnrichSample {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();
	private static String SIGEL_DUMP_LOCATION =
			Constants.TRANSFORMATION_INPUT + "sigel.xml";
	private static String SIGEL_TEMP_FILES_LOCATION =
			Constants.TRANSFORMATION_OUTPUT;
	private static String DBS_LOCATION =
			CONFIG.getString("transformation.test.input.path") + "dbs.csv";
	private static String DUMP_XPATH =
			"/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;

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
				Constants.SIGEL_DUMP_TOP_LEVEL_TAG, Constants.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH,
				CONFIG.getString("transformation.test.output.path"));
		Sigel.processSigelSplitting(sourceFileOpener, SIGEL_DUMP_LOCATION);

		// DBS flow
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
