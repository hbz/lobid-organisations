package flow;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.ObjectTemplate;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.pipe.sort.TripleCount;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

@SuppressWarnings("javadoc")
public class CountEnriched {

	public static void main(String... args) {
		count();
	}

	static void count() {
		DirReader openSigel = new DirReader();
		StreamToTriples streamToTriples1 = new StreamToTriples();
		streamToTriples1.setRedirect(true);
		StreamToTriples flow1 = //
				Sigel.morphSigel(openSigel).setReceiver(streamToTriples1);

		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriples2 = new StreamToTriples();
		streamToTriples2.setRedirect(true);
		StreamToTriples flow2 = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriples2);

		CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		continueWith(flow1, wait);
		continueWith(flow2, wait);

		Dbs.processDbs(openDbs);
		Sigel.processSigel(openSigel);
	}

	private static void continueWith(StreamToTriples flow,
			CloseSupressor<Triple> wait) {
		TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // remove entries w/o Street
		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		TripleCollect collectTriples = new TripleCollect();
		Metamorph morph = new Metamorph("src/test/resources/count_enriched.xml");
		StreamToTriples triples = new StreamToTriples();
		TripleCount count = new TripleCount();
		count.setCountBy(Compare.PREDICATE);
		TripleSort sort = new TripleSort();
		ObjectTemplate<Triple> template = new ObjectTemplate<>("${s} ${o}");
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/test/resources/output/count_enriched_out.txt");

		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(collectTriples)//
				.setReceiver(morph)//
				.setReceiver(triples)//
				.setReceiver(count)//
				.setReceiver(sort)//
				.setReceiver(template)//
				.setReceiver(writer);
	}
}
