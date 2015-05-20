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

	/** @param args Not used */
	public static void main(String... args) {
		final FileOpener opener = new FileOpener();
		final JsonEncoder encoder = Helpers.createJsonEncoder(true);
		final ObjectWriter<String> writer =
				new ObjectWriter<>(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH + "dbs.out.json");
		morphDbs(opener)//
				.setReceiver(encoder)//
				.setReceiver(writer);
		processDbs(opener, Constants.DBS_LOCATION);
	}

	static Metamorph morphDbs(FileOpener opener) {
		opener.setEncoding("ISO-8859-1");
		final LineReader lines = new LineReader();
		final CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-dbs.xml");

		final Metamorph morphDbs = opener//
				.setReceiver(lines)//
				// .setReceiver(new ObjectLogger<>("Line: "))//
				.setReceiver(decoder)//
				// .setReceiver(new StreamLogger("CSV: "))//
				.setReceiver(morph);
		return morphDbs;
	}

	static void processDbs(FileOpener opener, String source) {
		opener.process(source);
		opener.closeStream();
	}
}
