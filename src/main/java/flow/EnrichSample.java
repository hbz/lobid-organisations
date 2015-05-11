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
		processSample();
	}

	static void processSample() {
		FileOpener openSigelDump = new FileOpener();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigelDump, xmlSplitter, DUMP_XPATH).setReceiver(
						streamToTriples1);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		Enrich.continueWith(flow1, wait);
		Enrich.continueWith(flow2, wait);

		Sigel.processSigel(openSigelDump, SIGEL_DUMP_LOCATION);
		Dbs.processDbs(openDbs, DBS_LOCATION);
	}
}
