package flow;

import org.culturegraph.mf.morph.Metamorph;
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
		// morphSigelDump(Constants.MAIN_RESOURCES_PATH);
		// morphSigelUpdates("2013-01-01", "2013-12-31",
		// "sigel-updates2013.out.json");
		// morphSigelUpdates("2014-01-01", getToday(),
		// "sigel-updates2014.out.json");

		final FileOpener dumpOpener = setupSigelMorphDump();
		final OaiPmhOpener update1Opener =
				setupSigelMorphUpdate("2013-01-01", "2013-12-31",
						"sigel-updates2013.out.json");
		final OaiPmhOpener update2Opener =
				setupSigelMorphUpdate("2014-01-01", "2014-12-31",
						"sigel-updates2014.out.json");
		final OaiPmhOpener update3Opener =
				setupSigelMorphUpdate("2015-01-01", Helpers.getToday(),
						"sigel-updates2015.out.json");

		processSigel(dumpOpener, Constants.SIGEL_DUMP_LOCATION);
		processSigel(update1Opener, Constants.SIGEL_DNB_REPO);
		processSigel(update2Opener, Constants.SIGEL_DNB_REPO);
		processSigel(update3Opener, Constants.SIGEL_DNB_REPO);
	}

	static Metamorph setupSigelMorph(final Opener opener,
			final XmlEntitySplitter aEntitySplitter, String aXPath) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final XmlTee tee = new XmlTee();
		final XmlFilenameWriter xmlFilenameWriter =
				createXmlFilenameWriter(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH, aXPath);

		final PicaXmlHandler xmlHandler = new PicaXmlHandler();
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-sigel.xml");
		opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(tee);
		tee.addReceiver(aEntitySplitter);
		tee.addReceiver(xmlHandler);
		aEntitySplitter.setReceiver(xmlFilenameWriter);
		return xmlHandler.setReceiver(morph);
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

	static void processSigel(final Opener opener, final String source) {
		opener.process(source);
		opener.closeStream();
	}

	private static FileOpener setupSigelMorphDump() {
		final FileOpener opener = new FileOpener();
		final XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		final Metamorph dumpMorph =
				setupSigelMorph(opener, xmlSplitter, "/"
						+ Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH);
		final String sigelOutPath =
				Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH
						+ "sigel-dump.out.json";
		dumpMorph //
				.setReceiver(Helpers.createJsonEncoder(true)) //
				.setReceiver(new ObjectWriter<>(sigelOutPath));
		return opener;
	}

	private static OaiPmhOpener setupSigelMorphUpdate(String start, String end,
			String outputFile) {
		final OaiPmhOpener opener = Helpers.createOaiPmhOpener(start, end);
		final XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_UPDATE_TOP_LEVEL_TAG,
						Constants.SIGEL_UPDATE_ENTITY);
		final Metamorph updatesMorph =
				setupSigelMorph(opener, xmlSplitter, "/"
						+ Constants.SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
						+ Constants.SIGEL_UPDATE_ENTITY + "/" + Constants.SIGEL_XPATH);
		final String sigelUpdatesOutPath =
				Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH + outputFile;
		updatesMorph //
				.setReceiver(Helpers.createJsonEncoder(true)) //
				.setReceiver(new ObjectWriter<>(sigelUpdatesOutPath));
		return opener;
	}

}
