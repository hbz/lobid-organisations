/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

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
		final ObjectWriter<String> writer =
				new ObjectWriter<>(Enrich.DATA_OUTPUT_DIR + "dbs.out.json");
		morphDbs(opener)//
				.setReceiver(encoder)//
				.setReceiver(writer);
		processDbs(opener, Enrich.DATA_INPUT_DIR + "dbs.csv");
	}

	static Metamorph morphDbs(final FileOpener opener) {
		opener.setEncoding("UTF-8");
		final LineReader lines = new LineReader();
		final CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		final Metamorph morph = new Metamorph("morph-dbs.xml");
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
