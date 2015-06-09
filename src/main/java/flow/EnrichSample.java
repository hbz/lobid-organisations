package flow;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

/**
 * @author Simon Ritter (SBRitter)
 * 
 *         For tests: sample data only, no updates
 *
 */
public class EnrichSample {

	private static String mSigelDumpLocation =
			ElasticsearchAuxiliary.TEST_RESOURCES_PATH
					+ ElasticsearchAuxiliary.SIGEL_DUMP_LOCATION;
	private static String mDbsLocation =
			ElasticsearchAuxiliary.TEST_RESOURCES_PATH
					+ ElasticsearchAuxiliary.DBS_LOCATION;

	/**
	 * @param args not used
	 */
	public static void main(String... args) {
		processSample(ElasticsearchAuxiliary.TEST_RESOURCES_PATH
				+ "output/enriched.out.json");
	}

	static void processSample(final String aOutputPath) {
		FileOpener openSigelDump = new FileOpener();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigelDump).setReceiver(streamToTriples1);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		Enrich.continueWith(flow1, wait, aOutputPath);
		Enrich.continueWith(flow2, wait, aOutputPath);

		Sigel.processSigel(openSigelDump, mSigelDumpLocation);
		Dbs.processDbs(openDbs, mDbsLocation);
	}
}
