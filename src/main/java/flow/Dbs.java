package flow;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.CsvDecoder;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.LineReader;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;

/**
 * Initial simple transformation from DBS CSV to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Dbs {

	/**
	 * @param args Not used
	 */
	public static void main(final String... args) {
		final FileOpener opener = new FileOpener();
		final JsonEncoder encoder = new JsonEncoder();
		encoder.setPrettyPrinting(true);
		final ObjectWriter<String> writer = new ObjectWriter<>(
				ElasticsearchAuxiliary.MAIN_RESOURCES_PATH + "output/dbs.out.json");
		morphDbs(opener)//
				.setReceiver(encoder)//
				.setReceiver(writer);
		processDbs(opener, ElasticsearchAuxiliary.DBS_LOCATION);
	}

	static Metamorph morphDbs(final FileOpener opener) {
		opener.setEncoding("ISO-8859-1");
		final LineReader lines = new LineReader();
		final CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		final Metamorph morph = new Metamorph(
				ElasticsearchAuxiliary.MAIN_RESOURCES_PATH + "morph-dbs.xml");

		final Metamorph morphDbs = opener//
				.setReceiver(lines)//
				.setReceiver(decoder)//
				.setReceiver(morph);
		return morphDbs;
	}

	static void processDbs(final FileOpener opener, final String source) {
		opener.process(source);
		opener.closeStream();
	}
}
