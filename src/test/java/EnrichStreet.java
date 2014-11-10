import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.JsonToElasticsearchBulk;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class EnrichStreet {

	/**
	 * @param args Not used
	 */
	public static void main(String... args) {
		/* Run both preparatory pipelines standalone for debugging, doc etc. */

		DbsStreet.main();
		SigelStreet.main();

		/* Run the actual enrichment pipeline, which includes the previous: */
		process();

		/* Count the data */
		CountDBS.main();
		CountSigel.main();
		CountEnriched.main();
	}

	static void process() {
		DirReader openSigel = new DirReader();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				SigelStreet.morphSigel(openSigel).setReceiver(streamToTriples1);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				DbsStreet.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		continueWith(flow1, wait);
		continueWith(flow2, wait);

		DbsStreet.processDbs(openDbs);
		SigelStreet.processSigel(openSigel);
	}

	private static void continueWith(StreamToTriples flow,
			CloseSupressor<Triple> wait) {
		TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // remove entries w/o Street
		Metamorph morph =
				new Metamorph("src/test/resources/street-morph-enriched.xml");

		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		TripleCollect collectTriples = new TripleCollect();
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/test/resources/output/street-enriched.out.json");
		JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("new-id", "dbs", "organisations");

		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(collectTriples)//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(writer);
	}
}
