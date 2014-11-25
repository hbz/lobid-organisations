package flow;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.xml.PicaXmlHandler;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.stream.source.Opener;

/**
 * Initial simple transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Sigel {

	/** @param args Not used */
	public static void main(String... args) {
		morphSigelDump();
		morphSigelUpdates("2013-01-01", "2013-12-31", "sigel-updates2013.out.json");
		morphSigelUpdates("2014-01-01", getToday(), "sigel-updates2014.out.json");
	}

	static Metamorph morphSigel(Opener opener) {
		XmlDecoder xmlDecoder = new XmlDecoder();
		PicaXmlHandler xmlHandler = new PicaXmlHandler();
		Metamorph morph = new Metamorph("src/main/resources/morph-sigel.xml");
		Metamorph sigelMorph = opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(xmlHandler)//
				.setReceiver(morph);//
		return sigelMorph;
	}

	static OaiPmhOpener createOaiPmhOpener(String start, String end) {
		OaiPmhOpener opener = new OaiPmhOpener();
		opener.setDateFrom(start);
		opener.setDateUntil(end);
		opener.setMetadataPrefix("PicaPlus-xml");
		opener.setSetSpec("bib");
		return opener;
	}

	static void processSigel(Opener opener, String source) {
		opener.process(source);
		opener.closeStream();
	}

	static String getToday() {
		String dateFormat = "yyyy-MM-dd";
		Calendar calender = Calendar.getInstance();
		SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);
		return simpleDate.format(calender.getTime());
	}

	private static Metamorph morphSigelDump() {
		FileOpener opener = new FileOpener();
		Metamorph dumpMorph = morphSigel(opener);
		writeOut(dumpMorph, "src/main/resources/output/sigel-dump.out.json");
		processSigel(opener, "src/main/resources/input/sigel.xml");
		return dumpMorph;
	}

	private static Metamorph morphSigelUpdates(String start, String end,
			String outputFile) {
		OaiPmhOpener opener = createOaiPmhOpener(start, end);
		Metamorph updatesMorph = morphSigel(opener);
		writeOut(updatesMorph, "src/main/resources/output/" + outputFile);
		processSigel(opener, "http://services.d-nb.de/oai/repository");
		return updatesMorph;
	}

	private static void writeOut(Metamorph morph, String path) {
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer = new ObjectWriter<>(path);
		morph.setReceiver(encodeJson)//
				.setReceiver(writer);
	}
}