package flow;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.xml.PicaXmlHandler;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.stream.source.Opener;
import org.lobid.lodmill.XmlEntitySplitter;
import org.lobid.lodmill.XmlFilenameWriter;
import org.lobid.lodmill.XmlTee;

/**
 * Initial simple transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Sigel {

	/** @param args Not used */
	public static void main(final String... args) {
		morphSigelDump(ElasticsearchAuxiliary.MAIN_RESOURCES_PATH);
		morphSigelUpdates("2013-01-01", "2013-12-31", "sigel-updates2013.out.json");
		morphSigelUpdates("2014-01-01", Helpers.getToday(),
				"sigel-updates2014.out.json");
	}

	static Metamorph morphSigel(final Opener opener) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final PicaXmlHandler xmlHandler = new PicaXmlHandler();
		final Metamorph morph =
				new Metamorph(ElasticsearchAuxiliary.MAIN_RESOURCES_PATH
						+ "morph-sigel.xml");
		final Metamorph sigelMorph = opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(xmlHandler)//
				.setReceiver(morph);//
		return sigelMorph;
	}

	static OaiPmhOpener createOaiPmhOpener(final String start, final String end) {
		final OaiPmhOpener opener = new OaiPmhOpener();
		opener.setDateFrom(start);
		opener.setDateUntil(end);
		opener.setMetadataPrefix("PicaPlus-xml");
		opener.setSetSpec("bib");
		return opener;
	}

	static void processSigel(final Opener opener, final String source) {
		opener.process(source);
		opener.closeStream();
	}

	private static Metamorph morphSigelDump(final String aResourcesPath) {
		final FileOpener opener = new FileOpener();
		final Metamorph dumpMorph = morphSigel(opener);
		writeOut(dumpMorph, ElasticsearchAuxiliary.MAIN_RESOURCES_PATH
				+ "output/sigel-dump.out.json");
		processSigel(opener, aResourcesPath
				+ ElasticsearchAuxiliary.SIGEL_DUMP_LOCATION);
		return dumpMorph;
	}

	private static Metamorph morphSigelUpdates(final String start,
			final String end, String outputFile) {
		final OaiPmhOpener opener = createOaiPmhOpener(start, end);
		final Metamorph updatesMorph = morphSigel(opener);
		writeOut(updatesMorph, ElasticsearchAuxiliary.MAIN_RESOURCES_PATH
				+ "output/" + outputFile);
		processSigel(opener, ElasticsearchAuxiliary.SIGEL_DNB_REPO);
		return updatesMorph;
	}

	static Metamorph setupSigelMorph(final Opener opener,
			final XmlEntitySplitter aEntitySplitter, final String aXPath) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final XmlTee tee = new XmlTee();
		final XmlFilenameWriter xmlFilenameWriter =
				createXmlFilenameWriter(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH, aXPath);
		final PicaXmlHandler xmlHandler = new PicaXmlHandler();
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-sigel.xml");

		opener //
				.setReceiver(xmlDecoder) //
				.setReceiver(tee);
		tee.addReceiver(aEntitySplitter);
		tee.addReceiver(xmlHandler);
		aEntitySplitter //
				.setReceiver(xmlFilenameWriter);
		return xmlHandler //
				.setReceiver(morph);
	}

	private static XmlFilenameWriter createXmlFilenameWriter(String aOutputPath,
			String aXPath) {
		final XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(0);
		xmlFilenameWriter.setEndIndex(2);
		xmlFilenameWriter.setTarget(aOutputPath);
		xmlFilenameWriter.setProperty(aXPath);
		return xmlFilenameWriter;
	}

	private static void writeOut(final Metamorph morph, final String path) {
		final JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		final ObjectWriter<String> writer = new ObjectWriter<>(path);
		morph.setReceiver(encodeJson)//
				.setReceiver(writer);
	}
}