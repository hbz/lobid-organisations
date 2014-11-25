package flow;

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
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.types.Triple;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Enrich {

	private static String sigelDumpLocation =
			"src/main/resources/input/sigel.xml";
	private static String sigelDnbRepo = "http://services.d-nb.de/oai/repository";

	/**
	 * @param args Not used
	 */
	public static void main(String... args) {
		/* Run both preparatory pipelines standalone for debugging, doc etc. */
		// Dbs.main();
		// Sigel.main();
		/* Run the actual enrichment pipeline, which includes the previous: */
		process();
	}

	static void process() {

		FileOpener openSigelDump = new FileOpener();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigelDump).setReceiver(streamToTriples1);

		OaiPmhOpener openSigelUpdates1 =
				Sigel.createOaiPmhOpener("2013-06-01", "2013-12-01");
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Sigel.morphSigel(openSigelUpdates1).setReceiver(streamToTriples2);

		OaiPmhOpener openSigelUpdates2 =
				Sigel.createOaiPmhOpener("2014-01-01", Sigel.getToday());
		StreamToTriples streamToTriples3 = new StreamToTriples();
		streamToTriples3.setRedirect(true);
		StreamToTriples flow3 = //
				Sigel.morphSigel(openSigelUpdates2).setReceiver(streamToTriples3);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples4 = new StreamToTriples();
		streamToTriples4.setRedirect(true);
		StreamToTriples flow4 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples4);

		CloseSupressor<Triple> wait = new CloseSupressor<>(4);
		continueWith(flow1, wait);
		continueWith(flow2, wait);
		continueWith(flow3, wait);
		continueWith(flow4, wait);

		Sigel.processSigel(openSigelDump, sigelDumpLocation);
		Sigel.processSigel(openSigelUpdates1, sigelDnbRepo);
		Sigel.processSigel(openSigelUpdates2, sigelDnbRepo);
		Dbs.processDbs(openDbs);
	}

	private static void continueWith(StreamToTriples flow,
			CloseSupressor<Triple> wait) {
		TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		Metamorph morph = new Metamorph("src/main/resources/morph-enriched.xml");
		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/output/enriched.out.json");
		JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("@id", "organisation", "organisations");
		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(writer);
	}

	/* For tests */
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
		continueWith(flow1, wait);
		continueWith(flow2, wait);

		Sigel.processSigel(openSigelDump, sigelDumpLocation);
		Dbs.processDbs(openDbs);
	}
}