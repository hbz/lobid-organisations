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

	private static String sigelDumpLocation =
			"src/main/resources/input/sigel.xml";
	private static String dbsLocation = "src/main/resources/input/dbs.csv";

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
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigelDump).setReceiver(streamToTriples1);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		Enrich.continueWith(flow1, wait);
		Enrich.continueWith(flow2, wait);

		Sigel.processSigel(openSigelDump, sigelDumpLocation);
		Dbs.processDbs(openDbs, dbsLocation);
	}
}
