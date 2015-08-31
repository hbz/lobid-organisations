package flow;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.lobid.lodmill.XmlEntitySplitter;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg), Simon Ritter (SBRitter)
 *
 */
public class Enrich {

	final static String DUMP_XPATH = "/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;

	/**
	 * @param args
	 *            start date of Sigel updates (date of Sigel base dump) and size
	 *            of update intervals in days
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static void main(String... args) throws IOException, NumberFormatException {
		if (args.length == 0) {
			args = new String[] { "2015-05-01", "100" };
		}
		String startOfUpdates = args[0];
		int intervalSize = Integer.parseInt(args[1]);
		String outputPath = Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH + "enriched.out.json";
		process(startOfUpdates, intervalSize, Constants.MAIN_RESOURCES_PATH, outputPath);
	}

	static void process(String startOfUpdates, int intervalSize, final String aResourcesPath, final String aOutputPath)
			throws IOException {

		int updateIntervals = calculateIntervals(startOfUpdates, Helpers.getToday(), intervalSize);

		String sigelTempFilesLocation = Constants.MAIN_RESOURCES_PATH + Constants.OUTPUT_PATH;

		// SETUP DBS
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = Helpers.createTripleStream(true);
		final StreamToTriples flowDbs = //
		Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);
		Helpers.setupTripleStreamToWriter(flowDbs, aOutputPath);
		Dbs.processDbs(openDbs, aResourcesPath + Constants.DBS_LOCATION);

		// SETUP SIGEL DUMP
		final FileOpener openSigelDump = new FileOpener();
		final XmlEntitySplitter xmlSplitter = new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
				Constants.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(openSigelDump, xmlSplitter, DUMP_XPATH);

		// SETUP SIGEL UPDATE
		ArrayList<OaiPmhOpener> updateOpenerList = buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals);

		// SETUP PROCESSING OF SPLITTED AND UPDATED SIGEL XML FILES
		final FileOpener splitFileOpener = new FileOpener();
		final StreamToTriples streamToTriplesDump = Helpers.createTripleStream(true);
		Sigel.setupSigelMorph(splitFileOpener).setReceiver(streamToTriplesDump);
		Helpers.setupTripleStreamToWriter(streamToTriplesDump, aOutputPath);

		// PROCESS SIGEL
		Sigel.processSigelSource(openSigelDump, aResourcesPath + Constants.SIGEL_DUMP_LOCATION);
		for (OaiPmhOpener updateOpener : updateOpenerList) {
			Sigel.processSigelSource(updateOpener, Constants.SIGEL_DNB_REPO);
		}
		Sigel.processSigelTriples(splitFileOpener, sigelTempFilesLocation);

	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(int intervalSize, String startOfUpdates,
			int updateIntervals) {
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
			final OaiPmhOpener openSigelUpdates = Helpers.createOaiPmhOpener(start, end);
			final XmlEntitySplitter xmlSplitter = new XmlEntitySplitter(Constants.SIGEL_UPDATE_TOP_LEVEL_TAG,
					Constants.SIGEL_UPDATE_ENTITY);
			final String updateXPath = "/" + Constants.SIGEL_UPDATE_TOP_LEVEL_TAG + "/" + Constants.SIGEL_UPDATE_ENTITY
					+ "/" + Constants.SIGEL_XPATH;
			Sigel.setupSigelSplitting(openSigelUpdates, xmlSplitter, updateXPath);

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

	private static int calculateIntervals(String start, String end, int intervalSize) {
		final LocalDate startDate = LocalDate.parse(start);
		final LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}
}
