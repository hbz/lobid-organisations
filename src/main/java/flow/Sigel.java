package flow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.xml.PicaXmlHandler;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;
import org.culturegraph.mf.stream.source.Opener;
import org.lobid.lodmill.XmlEntitySplitter;
import org.lobid.lodmill.XmlFilenameWriter;

/**
 * Initial simple transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Sigel {

	static XmlFilenameWriter setupSigelSplitting(final Opener opener, final XmlEntitySplitter aEntitySplitter,
			String aXPath, final String aOutputPath) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final XmlFilenameWriter xmlFilenameWriter = createXmlFilenameWriter(aOutputPath, aXPath);
		return opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(aEntitySplitter)//
				.setReceiver(xmlFilenameWriter);
	}

	static Metamorph setupSigelMorph(final Opener opener) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final PicaXmlHandler xmlHandler = new PicaXmlHandler();
		final Metamorph morph = new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-sigel.xml");
		return opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(xmlHandler)//
				.setReceiver(morph);
	}

	private static XmlFilenameWriter createXmlFilenameWriter(String aOutputPath, String aXPath) {
		final XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(0);
		xmlFilenameWriter.setEndIndex(2);
		xmlFilenameWriter.setTarget(aOutputPath);
		xmlFilenameWriter.setProperty(aXPath);
		return xmlFilenameWriter;
	}

	static void processSigelSource(final Opener aSourceFileOpener, final String aSource) {
		aSourceFileOpener.process(aSource);
		aSourceFileOpener.closeStream();
	}

	static void processSigelTriples(final Opener aSplitFileOpener, final String aTempFilesDir) throws IOException {
		Files.walk(Paths.get(aTempFilesDir))//
				.filter(Files::isRegularFile)//
				.filter(file -> file.toString().endsWith(".xml"))//
				.collect(Collectors.toList()).forEach(path -> {
					aSplitFileOpener.process(path.toString());

				});
		aSplitFileOpener.closeStream();
	}

}
