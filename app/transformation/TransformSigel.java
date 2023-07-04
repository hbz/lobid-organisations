/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import org.metafacture.framework.ObjectReceiver;
import org.metafacture.framework.helpers.DefaultObjectPipe;
import org.metafacture.json.JsonEncoder;
import org.metafacture.metafix.Metafix;
import org.metafacture.plumbing.StreamTee;
import org.metafacture.triples.StreamToTriples;
import org.metafacture.biblio.pica.PicaXmlHandler;
import org.metafacture.xml.XmlDecoder;
import org.metafacture.triples.TripleFilter;
import org.metafacture.xml.XmlElementSplitter;
import org.metafacture.triples.TripleCollect;
import org.metafacture.io.ObjectWriter;
import org.metafacture.xml.XmlFilenameWriter;
import org.metafacture.io.FileOpener;
import org.metafacture.biblio.OaiPmhOpener;


import controllers.Application;
import play.Logger;

/**
 * Transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class TransformSigel {

	static final String DUMP_TOP_LEVEL_TAG = "collection";
	static final String DUMP_ENTITY = "record";
	static final String UPDATE_TOP_LEVEL_TAG = "harvest";
	static final String UPDATE_ENTITY = "metadata";
	static final String XPATH =
			"/*[local-name() = 'record']/*[local-name() = 'global']/*[local-name() = 'tag'][@id='008H']/*[local-name() = 'subf'][@id='e']";
	static final String DUMP_XPATH = "/" + DUMP_TOP_LEVEL_TAG + "/" + XPATH;

	static void process(String startOfUpdates, int intervalSize,
			final String outputPath, String geoLookupServer) throws IOException {
		splitUpSigelDump();
		final StreamTee tee = new StreamTee();
		final FileOpener splitFileOpener = new FileOpener();
		StreamToTriples streamToTriples = new StreamToTriples();
		streamToTriples.setRedirect(true);
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setReceiver(TransformAll.esBulk()//
				.setReceiver(new ObjectWriter<>(outputPath)));
		JsonEncoder encodePrettyJson = new JsonEncoder();
		encodePrettyJson.setPrettyPrinting(true);
		encodePrettyJson.setReceiver(new ObjectWriter<>(outputPath + "-pretty.json"));
		splitFileOpener//
				.setReceiver(new XmlDecoder())//
				.setReceiver(new PicaXmlHandler())//
				.setReceiver(new Metafix("conf/fix-sigel.fix"))//
				.setReceiver(streamToTriples)//
				.setReceiver(tripleFilter)//
				.setReceiver(new TripleCollect())//
				.setReceiver(TransformAll.fixEnriched(geoLookupServer))//
				.setReceiver(tee);
		tee //
				.addReceiver(encodeJson) //
				.addReceiver(encodePrettyJson);
		if (!startOfUpdates.isEmpty()) {
			processSigelUpdates(startOfUpdates, intervalSize);
		}
		Files.walk(Paths.get(TransformAll.DATA_OUTPUT_DIR))//
				.filter(Files::isRegularFile)//
				.filter(file -> file.toString().endsWith(".xml"))//
				.collect(Collectors.toList()).forEach(path -> {
					splitFileOpener.process(path.toString());
				});
		splitFileOpener.closeStream();
	}

	private static void splitUpSigelDump() {
		final FileOpener dumpFileOpener = new FileOpener();
		dumpFileOpener//
				.setReceiver(new XmlDecoder())//
				.setReceiver(new XmlElementSplitter(DUMP_TOP_LEVEL_TAG, DUMP_ENTITY))//
				.setReceiver(
						xmlFilenameWriter(TransformAll.DATA_OUTPUT_DIR, DUMP_XPATH));
		dumpFileOpener.process(TransformAll.DATA_INPUT_DIR + "sigel.xml");
		dumpFileOpener.closeStream();
	}

	private static void processSigelUpdates(String startOfUpdates,
			int intervalSize) {
		int updateIntervals =
				calculateIntervals(startOfUpdates, getToday(), intervalSize);
		ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals);
		for (OaiPmhOpener updateOpener : updateOpenerList) {
			updateOpener.process(
					Application.CONFIG.getString("transformation.sigel.repository"));
			updateOpener.closeStream();
		}
	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(int intervalSize,
			String startOfUpdates, int updateIntervals) {
		String start = startOfUpdates;
		String end = addDays(start, intervalSize);
		final ArrayList<OaiPmhOpener> updateOpenerList = new ArrayList<>();

		// There has to be at least one interval
		int intervals;
		if (updateIntervals == 0)
			intervals = 1;
		else
			intervals = updateIntervals;

		for (int i = 0; i < intervals; i++) {
			final OaiPmhOpener openSigelUpdates = createOaiPmhOpener(start, end);
			final XmlElementSplitter xmlSplitter =
					new XmlElementSplitter(UPDATE_TOP_LEVEL_TAG, UPDATE_ENTITY);
			final String updateXPath =
					"/" + UPDATE_TOP_LEVEL_TAG + "/" + UPDATE_ENTITY + "/" + XPATH;
			setupSigelSplitting(openSigelUpdates, xmlSplitter, updateXPath,
					TransformAll.DATA_OUTPUT_DIR);

			updateOpenerList.add(openSigelUpdates);
			start = addDays(start, intervalSize);
			if (i == intervals - 2)
				end = getToday();
			else
				end = addDays(end, intervalSize);
		}

		return updateOpenerList;
	}

	/**
	 * @param start the start of updates formatted in yyyy-MM-dd
	 * @param end the end of updates formatted in yyyy-MM-dd
	 * @return a new OaiPmhOpener
	 */
	private static OaiPmhOpener createOaiPmhOpener(String start, String end) {
		OaiPmhOpener opener = new OaiPmhOpener();
		opener.setDateFrom(start);
		opener.setDateUntil(end);
		opener.setMetadataPrefix("PicaPlus-xml");
		opener.setSetSpec("bib");
		return opener;
	}

	private static String addDays(String start, int intervalSize) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String result = null;
		try {
			final Date startDate = dateFormat.parse(start);
			final Calendar calender = Calendar.getInstance();
			calender.setTime(startDate);
			calender.add(Calendar.DATE, intervalSize);
			result = dateFormat.format(calender.getTime());
		} catch (ParseException e) {
			Logger.warn("Couldn't add days", e);
		}
		return result;
	}

	private static int calculateIntervals(String startOfUpdates, String end,
			int intervalSize) {
		final LocalDate startDate = LocalDate.parse(startOfUpdates);
		final LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

	private static String getToday() {
		String dateFormat = "yyyy-MM-dd";
		Calendar calender = Calendar.getInstance();
		SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);
		return simpleDate.format(calender.getTime());
	}

	static XmlFilenameWriter setupSigelSplitting(final DefaultObjectPipe<String, ObjectReceiver<Reader>> opener,
			final XmlElementSplitter splitter, String xPath,
			final String outputPath) {
		final XmlDecoder xmlDecoder = new XmlDecoder();
		final XmlFilenameWriter xmlFilenameWriter =
				xmlFilenameWriter(outputPath, xPath);
		return opener//
				.setReceiver(xmlDecoder)//
				.setReceiver(splitter)//
				.setReceiver(xmlFilenameWriter);
	}

	private static XmlFilenameWriter xmlFilenameWriter(String outputPath,
			String xPath) {
		final XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(0);
		xmlFilenameWriter.setEndIndex(2);
		xmlFilenameWriter.setTarget(outputPath);
		xmlFilenameWriter.setProperty(xPath);
		return xmlFilenameWriter;
	}

}
