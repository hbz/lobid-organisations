import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.HttpOpener;
import org.culturegraph.mf.types.Triple;

/**
 * Initial simple merging of DBS and Sigel records based on their ISIL.
 * 
 * After merging, the result is transformed to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class SigelAndDbsToJson {

	/** @param args Not used */
	public static void main(String[] args) {

		HttpOpener openSigel = new HttpOpener();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				SigelToJson.morphSigel(openSigel).setReceiver(streamToTriples1);

		HttpOpener openDbs = new HttpOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				DbsToJson.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		continueWith(flow1, wait);
		continueWith(flow2, wait);

		DbsToJson.processDbs(openDbs);
		SigelToJson.processSigel(openSigel);
	}

	private static void continueWith(StreamToTriples flow,
			CloseSupressor<Triple> wait) {

		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/sigel-dbs.out.json");

		flow.setReceiver(wait)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(encodeJson)//
				.setReceiver(writer);
	}
}
