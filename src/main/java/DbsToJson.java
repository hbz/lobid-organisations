import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.CsvDecoder;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.LineReader;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.HttpOpener;

/**
 * Initial simple transformation from DBS CSV to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class DbsToJson {

	/** @param args Not used */
	public static void main(String[] args) {
		HttpOpener opener = new HttpOpener();
		JsonEncoder encoder = new JsonEncoder();
		encoder.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/dbs.out.json");
		morphDbs(opener)//
				.setReceiver(encoder)//
				.setReceiver(writer);
		processDbs(opener);
	}

	static Metamorph morphDbs(HttpOpener opener) {
		opener.setEncoding("ISO-8859-1");
		LineReader lines = new LineReader();
		CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		Metamorph morph = new Metamorph("src/main/resources/dbs.morph.xml");

		Metamorph morphDbs = opener//
				.setReceiver(lines)//
				// .setReceiver(new ObjectLogger<>("Line: "))//
				.setReceiver(decoder)//
				// .setReceiver(new StreamLogger("CSV: "))//
				.setReceiver(morph);
		return morphDbs;
	}

	static void processDbs(HttpOpener opener) {
		opener.process("http://test.lobid.org/assets/dbs.csv");
		opener.closeStream();
	}
}
