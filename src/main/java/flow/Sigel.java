package flow;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.reader.PicaXmlReader;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;

/**
 * Initial simple transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Sigel {

	/** @param args Not used */
	public static void main(String... args) {
		DirReader open = new DirReader();
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>("src/main/resources/output/sigel.out.json");
		morphSigel(open)//
				.setReceiver(encodeJson)//
				.setReceiver(writer);
		processSigel(open);
	}

	static Metamorph morphSigel(DirReader open) {
		open.setRecursive(false);
		open.setFilenamePattern(".*\\.xml");
		FileOpener openFile = new FileOpener();
		PicaXmlReader readPicaXml = new PicaXmlReader();
		Metamorph morph = new Metamorph("src/main/resources/morph-sigel.xml");
		return open//
				.setReceiver(openFile)//
				.setReceiver(readPicaXml)//
				.setReceiver(morph);//
	}

	static void processSigel(DirReader open) {
		open.process("src/main/resources/input/sigel/");
		open.closeStream();
	}
}
