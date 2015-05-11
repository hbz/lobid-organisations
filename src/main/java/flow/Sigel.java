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
	public static void main(String... args) {
		morphSigelDump();
		morphSigelUpdates("2013-01-01", "2013-12-31", "sigel-updates2013.out.json");
		morphSigelUpdates("2014-01-01", getToday(), "sigel-updates2014.out.json");
	}

	static Metamorph morphSigel(final Opener opener,
			final XmlEntitySplitter aEntitySplitter, String aXPath) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final XmlTee tee = new XmlTee();
		final XmlFilenameWriter xmlFilenameWriter =
				createXmlFilenameWriter(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH, aXPath);
		aEntitySplitter.setReceiver(xmlFilenameWriter);
		final PicaXmlHandler xmlHandler = new PicaXmlHandler();
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-sigel.xml");
		opener.setReceiver(xmlDecoder).setReceiver(tee);
		tee.addReceiver(aEntitySplitter);
		tee.addReceiver(xmlHandler);
		final Metamorph sigelMorph = xmlHandler.setReceiver(morph);//
		return sigelMorph;
	}

	private static XmlFilenameWriter createXmlFilenameWriter(String aOutputPath,
			String aXPath) {
		XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(0);
		xmlFilenameWriter.setEndIndex(2);
		xmlFilenameWriter.setTarget(aOutputPath);
		xmlFilenameWriter.setProperty(aXPath);
		return xmlFilenameWriter;
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
		XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		Metamorph dumpMorph =
				morphSigel(opener, xmlSplitter, "/"
						+ Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH);
		writeOut(dumpMorph, Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH
				+ "sigel-dump.out.json");
		processSigel(opener, Enrich.SIGEL_DUMP_LOCATION);
		return dumpMorph;
	}

	private static Metamorph morphSigelUpdates(String start, String end,
			String outputFile) {
		OaiPmhOpener opener = createOaiPmhOpener(start, end);
		XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_UPDATE_TOP_LEVEL_TAG,
						Constants.SIGEL_UPDATE_ENTITY);
		Metamorph updatesMorph =
				morphSigel(opener, xmlSplitter, "/"
						+ Constants.SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
						+ Constants.SIGEL_UPDATE_ENTITY + "/" + Constants.SIGEL_XPATH);
		writeOut(updatesMorph, Constants.MAIN_RESOURCES_PATH
				+ Constants.OUTPUT_PATH + outputFile);
		processSigel(opener, Enrich.SIGEL_DNB_REPO);
		return updatesMorph;
	}

	private static void writeOut(Metamorph morph, String path) {
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer = new ObjectWriter<>(path);
		morph.setReceiver(encodeJson).setReceiver(writer);
	}
}