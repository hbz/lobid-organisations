/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.CsvDecoder;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.JsonToElasticsearchBulk;
import org.culturegraph.mf.stream.converter.LineReader;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.converter.xml.PicaXmlHandler;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.sink.XmlFilenameWriter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;

import controllers.Application;
import play.Logger;
import transformation.GeoLookupMap.LookupType;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg), Simon Ritter (SBRitter)
 *
 */
public class Enrich {

	/** The output directory. If not set in config, system temp is used. */
	public static final String DATA_OUTPUT_DIR =
			Application.CONFIG.hasPath("data.output.dir")
					? Application.CONFIG.getString("data.output.dir")
					: System.getProperty("java.io.tmpdir") + "/lobid-organisations";

	static {
		new File(DATA_OUTPUT_DIR).mkdir();
	}

	/** The output file path. */
	public static final String DATA_OUTPUT_FILE = DATA_OUTPUT_DIR + "/"
			+ controllers.Application.CONFIG.getString("index.file.name");

	static final String DATA_INPUT_DIR =
			Application.CONFIG.getString("data.input.dir");

	static final String SIGEL_DUMP_TOP_LEVEL_TAG = "collection";
	static final String SIGEL_DUMP_ENTITY = "record";
	static final String SIGEL_UPDATE_TOP_LEVEL_TAG = "harvest";
	static final String SIGEL_UPDATE_ENTITY = "metadata";
	static final String SIGEL_XPATH =
			"/*[local-name() = 'record']/*[local-name() = 'global']/*[local-name() = 'tag'][@id='008H']/*[local-name() = 'subf'][@id='e']";
	static final String DUMP_XPATH =
			"/" + SIGEL_DUMP_TOP_LEVEL_TAG + "/" + SIGEL_XPATH;

	/**
	 * @param startOfUpdates Date from which updates should start
	 * @param intervalSize Days to load update for at once
	 * @param outputPath The path to which the output of transform should go
	 * @param geoServer The lookup server for geo data
	 * @throws IOException If dump and temp files cannot be read
	 */
	public static void process(String startOfUpdates, int intervalSize,
			final String outputPath, String geoServer) throws IOException {
		String dbsOutput = outputPath + "-dbs";
		String sigelOutput = outputPath + "-sigel";
		processDbs(dbsOutput, geoServer);
		processSigelDump();
		processSigelUpdates(startOfUpdates, intervalSize, sigelOutput, geoServer);
		try (FileWriter resultWriter = new FileWriter(outputPath)) {
			resultWriter.write(Files.readAllLines(Paths.get(dbsOutput)).stream()
					.collect(Collectors.joining("\n")));
			resultWriter.write("\n");
			resultWriter.write(Files.readAllLines(Paths.get(sigelOutput)).stream()
					.collect(Collectors.joining("\n")));
		}
	}

	private static void processDbs(final String outputPath,
			String geoLookupServer) {
		final FileOpener opener = new FileOpener();
		StreamToTriples streamToTriples = new StreamToTriples();
		streamToTriples.setRedirect(true);
		opener.setEncoding("ISO-8859-1");
		final CsvDecoder decoder = new CsvDecoder(';');
		decoder.setHasHeader(true);
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		opener//
				.setReceiver(new LineReader())//
				.setReceiver(decoder)//
				.setReceiver(new Metamorph("morph-dbs.xml"))//
				.setReceiver(streamToTriples)//
				.setReceiver(tripleFilter)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morphEnriched(geoLookupServer))//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		opener.process(DATA_INPUT_DIR + "dbs.csv");
		opener.closeStream();
	}

	private static void processSigelDump() {
		final FileOpener dumpFileOpener = new FileOpener();
		dumpFileOpener//
				.setReceiver(new XmlDecoder())//
				.setReceiver(
						new XmlElementSplitter(SIGEL_DUMP_TOP_LEVEL_TAG, SIGEL_DUMP_ENTITY))//
				.setReceiver(xmlFilenameWriter());
		dumpFileOpener.process(DATA_INPUT_DIR + "sigel.xml");
		dumpFileOpener.closeStream();
	}

	private static void processSigelUpdates(String startOfUpdates,
			int intervalSize, final String outputPath, String geoLookupServer)
			throws IOException {
		final FileOpener splitFileOpener = new FileOpener();
		StreamToTriples streamToTriples = new StreamToTriples();
		streamToTriples.setRedirect(true);
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		splitFileOpener//
				.setReceiver(new XmlDecoder())//
				.setReceiver(new PicaXmlHandler())//
				.setReceiver(new Metamorph("morph-sigel.xml"))//
				.setReceiver(streamToTriples)//
				.setReceiver(tripleFilter)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morphEnriched(geoLookupServer))//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		if (!startOfUpdates.isEmpty()) {
			processSigelUpdates(startOfUpdates, intervalSize);
		}
		Files.walk(Paths.get(DATA_OUTPUT_DIR))//
				.filter(Files::isRegularFile)//
				.filter(file -> file.toString().endsWith(".xml"))//
				.collect(Collectors.toList()).forEach(path -> {
					splitFileOpener.process(path.toString());
				});
		splitFileOpener.closeStream();
	}

	private static XmlFilenameWriter xmlFilenameWriter() {
		final XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(0);
		xmlFilenameWriter.setEndIndex(2);
		xmlFilenameWriter.setTarget(DATA_OUTPUT_DIR);
		xmlFilenameWriter.setProperty(DUMP_XPATH);
		return xmlFilenameWriter;
	}

	private static JsonToElasticsearchBulk esBulk() {
		final JsonToElasticsearchBulk esBulk = new JsonToElasticsearchBulk("id",
				Application.CONFIG.getString("index.es.type"),
				Application.CONFIG.getString("index.es.name"));
		return esBulk;
	}

	private static Metamorph morphEnriched(String geoLookupServer) {
		final Metamorph morphEnriched = new Metamorph("morph-enriched.xml");
		if (geoLookupServer != null && !geoLookupServer.isEmpty()) {
			morphEnriched.putMap("addLatMap", new GeoLookupMap(LookupType.LAT));
			morphEnriched.putMap("addLongMap", new GeoLookupMap(LookupType.LON));
		}
		return morphEnriched;
	}

	private static void processSigelUpdates(String startOfUpdates,
			int intervalSize) {
		int updateIntervals =
				calculateIntervals(startOfUpdates, getToday(), intervalSize);
		ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals);
		for (OaiPmhOpener updateOpener : updateOpenerList) {
			Sigel.processSigelSplitting(updateOpener,
					Application.CONFIG.getString("transformation.sigel.repository"));
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
			final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
					SIGEL_UPDATE_TOP_LEVEL_TAG, SIGEL_UPDATE_ENTITY);
			final String updateXPath = "/" + SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
					+ SIGEL_UPDATE_ENTITY + "/" + SIGEL_XPATH;
			Sigel.setupSigelSplitting(openSigelUpdates, xmlSplitter, updateXPath,
					DATA_OUTPUT_DIR);

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

}
