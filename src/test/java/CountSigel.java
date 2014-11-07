import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.ObjectTemplate;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCount;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.reader.PicaXmlReader;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;

@SuppressWarnings("javadoc")
public class CountSigel {

	public static void main(String... args) {
		DirReader opener = new DirReader();
		opener.setRecursive(false);
		opener.setFilenamePattern(".*\\.xml");
		FileOpener openFile = new FileOpener();
		PicaXmlReader readPicaXml = new PicaXmlReader();
		Metamorph morph = new Metamorph("src/test/resources/count_sigel.xml");

		StreamToTriples triples = new StreamToTriples();
		TripleCount count = new TripleCount();
		count.setCountBy(Compare.PREDICATE);
		TripleSort sort = new TripleSort();
		ObjectTemplate<Triple> template = new ObjectTemplate<>("${s} ${o}");
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/test/resources/output/count_sigel_out.txt");

		opener//
				.setReceiver(openFile)//
				.setReceiver(readPicaXml)//
				.setReceiver(morph)//
				.setReceiver(triples)//
				.setReceiver(count)//
				.setReceiver(sort)//
				.setReceiver(template)//
				.setReceiver(writer);

		opener.process("src/main/resources/input/sigel/");
		opener.closeStream();
	}
}
