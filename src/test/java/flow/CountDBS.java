package flow;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.CsvDecoder;
import org.culturegraph.mf.stream.converter.LineReader;
import org.culturegraph.mf.stream.converter.ObjectTemplate;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCount;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

@SuppressWarnings("javadoc")
public class CountDBS {

	public static void main(String... args) {
		FileOpener opener = new FileOpener();
		opener.setEncoding("ISO-8859-1");
		LineReader lines = new LineReader();
		CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		Metamorph morph = new Metamorph("src/test/resources/count_dbs.xml");

		StreamToTriples triples = new StreamToTriples();
		TripleCount count = new TripleCount();
		count.setCountBy(Compare.PREDICATE);
		TripleSort sort = new TripleSort();
		ObjectTemplate<Triple> template = new ObjectTemplate<>("${s} ${o}");
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/test/resources/output/count_dbs_out.txt");

		opener//
				.setReceiver(lines)//
				.setReceiver(decoder)//
				.setReceiver(morph)//
				.setReceiver(triples)//
				.setReceiver(count)//
				.setReceiver(sort)//
				.setReceiver(template)//
				.setReceiver(writer);

		opener.process("src/main/resources/input/dbs.csv");
		opener.closeStream();
	}

}
