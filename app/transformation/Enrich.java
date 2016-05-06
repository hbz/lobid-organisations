package transformation;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.types.Triple;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Simple enrichment of DBS records with Sigel data based on the DBS ID.
 * 
 * After enrichment, the result is transformed to JSON for ES indexing.
 * 
 * @author Fabian Steeg (fsteeg), Simon Ritter (SBRitter)
 *
 */
public class Enrich {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();

	static final String DATA_INPUT_DIR = "app/resources/input/";
	static final String DATA_OUTPUT_DIR = "app/resources/output/";
	static final String MORPH_DIR = "app/resources/";

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
	 * @param aOutputPath The path to which the output of transform should go
	 * @param geoLookupServer The lookup server for geo data
	 * @throws IOException If dump and temp files cannot be read
	 */
	public static void process(String startOfUpdates, int intervalSize,
			final String aOutputPath, String geoLookupServer) throws IOException {

		final CloseSupressor<Triple> wait = new CloseSupressor<>(2);
		final TripleSort sortTriples = new TripleSort();
		final TripleRematch rematchTriples = new TripleRematch("isil");

		final String sigelTempFilesLocation = DATA_OUTPUT_DIR;

		// SETUP SIGEL DUMP
		final FileOpener openSigelDump = new FileOpener();
		final XmlElementSplitter xmlSplitter =
				new XmlElementSplitter(SIGEL_DUMP_TOP_LEVEL_TAG, SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(openSigelDump, xmlSplitter, DUMP_XPATH,
				DATA_OUTPUT_DIR);

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
		Sigel.processSigelSplitting(openSigelDump, DATA_INPUT_DIR + "sigel.xml");
		if (!startOfUpdates.isEmpty()) {
			processSigelUpdates(startOfUpdates, intervalSize);
		}
		Sigel.processSigelMorph(splitFileOpener, sigelTempFilesLocation);

		Dbs.processDbs(openDbs, DATA_INPUT_DIR + "dbs.csv");
	}

	private static void processSigelUpdates(String startOfUpdates,
			int intervalSize) {
		int updateIntervals =
				calculateIntervals(startOfUpdates, Helpers.getToday(), intervalSize);
		ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals);
		for (OaiPmhOpener updateOpener : updateOpenerList) {
			Sigel.processSigelSplitting(updateOpener,
					CONFIG.getString("transformation.sigel.repository"));
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
			final OaiPmhOpener openSigelUpdates =
					Helpers.createOaiPmhOpener(start, end);
			final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
					SIGEL_UPDATE_TOP_LEVEL_TAG, SIGEL_UPDATE_ENTITY);
			final String updateXPath = "/" + SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
					+ SIGEL_UPDATE_ENTITY + "/" + SIGEL_XPATH;
			Sigel.setupSigelSplitting(openSigelUpdates, xmlSplitter, updateXPath,
					DATA_OUTPUT_DIR);

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

	private static int calculateIntervals(String startOfUpdates, String end,
			int intervalSize) {
		final LocalDate startDate = LocalDate.parse(startOfUpdates);
		final LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

}
