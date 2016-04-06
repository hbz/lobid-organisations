package flow;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.types.Triple;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg), Simon Ritter (SBRitter)
 *
 */
public class Enrich {

	final static String DUMP_XPATH =
			"/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;

	/**
	 * <p>
	 * Supports three optional parameters:
	 * </p>
	 * <ol>
	 * <li>start date of Sigel updates (date of Sigel base dump), e.g.
	 * "2013-06-01"</li>
	 * <li>size of update intervals in days e.g. "100"</li>
	 * <li>URL of the geo lookup server, e.g. "http://gaia.hbz-nrw.de:7400"</li>
	 * </ol>
	 * <p>
	 * Pass either 1) nothing, 2) date, 3) date and size, or 4) date, size, and
	 * server. If no date is given, no updates are pulled. If no size is given, a
	 * default value is used. If no server is given, no geo lookup is performed.
	 * </p>
	 *
	 * @param args The date, size, and URL parameters
	 */
	public static void main(String... args) {
		try {
			Optional<String> startOfUpdates =
					args.length > 0 ? Optional.of(args[0]) : Optional.empty();
			String intervalSize = args.length > 1 ? args[1] : "100";
			Optional<String> geoLookupServer =
					args.length > 2 ? Optional.of(args[2]) : Optional.empty();
			String outputPath = Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH
					+ "enriched.out.json";
			process(startOfUpdates, Integer.parseInt(intervalSize),
					Constants.MAIN_RESOURCES_PATH, outputPath, geoLookupServer);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static void process(Optional<String> startOfUpdates, int intervalSize,
			final String aResourcesPath, final String aOutputPath,
			Optional<String> geoLookupServer) throws IOException {

		final CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		final TripleSort sortTriples = new TripleSort();
		final TripleRematch rematchTriples = new TripleRematch("isil");

		final String sigelTempFilesLocation =
				Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH;

		// SETUP SIGEL DUMP
		final FileOpener openSigelDump = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				Constants.SIGEL_DUMP_TOP_LEVEL_TAG, Constants.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(openSigelDump, xmlSplitter, DUMP_XPATH,
				Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH);

		// SETUP PROCESSING OF SPLITTED AND UPDATED SIGEL XML FILES
		final FileOpener splitFileOpener = new FileOpener();
		final StreamToTriples streamToTriplesDump =
				Helpers.createTripleStream(true);
		Sigel.setupSigelMorph(splitFileOpener).setReceiver(streamToTriplesDump);
		Helpers.setupTripleStreamToWriter(streamToTriplesDump, wait, sortTriples,
				rematchTriples, aOutputPath, geoLookupServer);

		// SETUP DBS
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = Helpers.createTripleStream(true);
		final StreamToTriples flowDbs = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);
		Helpers.setupTripleStreamToWriter(flowDbs, wait, sortTriples,
				rematchTriples, aOutputPath, geoLookupServer);

		// PROCESS SIGEL
		Sigel.processSigelSplitting(openSigelDump,
				aResourcesPath + Constants.SIGEL_DUMP_LOCATION);
		processSigelUpdates(startOfUpdates, intervalSize);
		Sigel.processSigelMorph(splitFileOpener, sigelTempFilesLocation);

		Dbs.processDbs(openDbs, aResourcesPath + Constants.DBS_LOCATION);
	}

	private static void processSigelUpdates(Optional<String> startOfUpdates,
			int intervalSize) {
		if (startOfUpdates.isPresent()) {
			int updateIntervals =
					calculateIntervals(startOfUpdates, Helpers.getToday(), intervalSize);
			ArrayList<OaiPmhOpener> updateOpenerList =
					buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals);
			for (OaiPmhOpener updateOpener : updateOpenerList) {
				Sigel.processSigelSplitting(updateOpener, Constants.SIGEL_DNB_REPO);
			}
		}
	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(int intervalSize,
			Optional<String> startOfUpdates, int updateIntervals) {
		String start = startOfUpdates.get();
		String end = addDays(start, intervalSize);
		final ArrayList<OaiPmhOpener> updateOpenerList = new ArrayList<>();

		// There has to be at least one interval
		int intervals;
		if (updateIntervals == 0)
			intervals = 1;
		else
			intervals = updateIntervals;

		for (int i = 0; i < intervals; i++) {
			final OaiPmhOpener openSigelUpdates =
					Helpers.createOaiPmhOpener(start, end);
			final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
					Constants.SIGEL_UPDATE_TOP_LEVEL_TAG, Constants.SIGEL_UPDATE_ENTITY);
			final String updateXPath = "/" + Constants.SIGEL_UPDATE_TOP_LEVEL_TAG
					+ "/" + Constants.SIGEL_UPDATE_ENTITY + "/" + Constants.SIGEL_XPATH;
			Sigel.setupSigelSplitting(openSigelUpdates, xmlSplitter, updateXPath,
					Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH);

			updateOpenerList.add(openSigelUpdates);
			start = addDays(start, intervalSize);
			if (i == intervals - 2)
				end = Helpers.getToday();
			else
				end = addDays(end, intervalSize);
		}

		return updateOpenerList;
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
			e.printStackTrace();
		}
		return result;
	}

	private static int calculateIntervals(Optional<String> startOfUpdates,
			String end, int intervalSize) {
		final LocalDate startDate = LocalDate.parse(startOfUpdates.get());
		final LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

}
