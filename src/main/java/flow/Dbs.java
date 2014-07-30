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
		FileOpener opener = new FileOpener();
		JsonEncoder encoder = new JsonEncoder();
		encoder.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/output/dbs.out.json");
		morphDbs(opener)//
				.setReceiver(encoder)//
				.setReceiver(writer);
		processDbs(opener);
	}

	static Metamorph morphDbs(FileOpener opener) {
		opener.setEncoding("ISO-8859-1");
		LineReader lines = new LineReader();
		CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		Metamorph morph = new Metamorph("src/main/resources/morph-dbs.xml");

		Metamorph morphDbs = opener//
				.setReceiver(lines)//
				// .setReceiver(new ObjectLogger<>("Line: "))//
				.setReceiver(decoder)//
				// .setReceiver(new StreamLogger("CSV: "))//
				.setReceiver(morph);
		return morphDbs;
	}

	static void processDbs(FileOpener opener) {
		opener.process("src/main/resources/input/dbs.csv");
		opener.closeStream();
	}
}
