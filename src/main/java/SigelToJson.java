import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.reader.PicaXmlReader;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.HttpOpener;

/**
 * Initial simple transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class SigelToJson {

	/** @param args Not used */
	public static void main(String[] args) {
		HttpOpener open = new HttpOpener();
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/sigel.out.json");
		morphSigel(open)//
				.setReceiver(encodeJson)//
				.setReceiver(writer);
		processSigel(open);
	}

	static Metamorph morphSigel(HttpOpener open) {
		PicaXmlReader readPicaXml = new PicaXmlReader();
		Metamorph morph = new Metamorph("src/main/resources/sigel.morph.xml");
		return open//
				.setReceiver(readPicaXml)//
				.setReceiver(morph)//
		;
	}

	static void processSigel(HttpOpener open) {
		open.process("http://services.d-nb.de/oai/repository"
				+ "?verb=ListRecords&metadataPrefix=PicaPlus-xml&from=2013-08-11&until=2013-09-12&set=bib");
		open.closeStream();
	}
}
