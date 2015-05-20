package flow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.JsonToElasticsearchBulk;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.sort.AbstractTripleSort.Compare;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.types.Triple;
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

	/**
	 * @param args start date of Sigel updates (date of Sigel base dump) and size
	 *          of update intervals in days
	 */
	public static void main(String... args) {
		if (args.length == 0) {
			args = new String[] { "2015-05-01", "100" };
		}
		String startOfUpdates = args[0];
		int intervalSize = Integer.parseInt(args[1]);
		process(startOfUpdates, intervalSize);
	}

	static void process(String startOfUpdates, int intervalSize) {
		int updateIntervals =
				calculateIntervals(startOfUpdates, Helpers.getToday(), intervalSize);
		final CloseSupressor<Triple> wait =
				new CloseSupressor<>(updateIntervals + 2);

		// Setup Sigel Dump
		final FileOpener openSigelDump = new FileOpener();
		final StreamToTriples streamToTriplesDump =
				Helpers.createTripleStream(true);
		final String dumpXPath =
				"/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;
		final XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		final StreamToTriples flowSigelDump = //
				Sigel.setupSigelMorph(openSigelDump, xmlSplitter, dumpXPath)
						.setReceiver(streamToTriplesDump);
		setupTripleStreamToWriter(flowSigelDump, wait);

		// Setup Sigel Update
		final ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals, wait);

		// Setup Dbs
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = Helpers.createTripleStream(true);
		final StreamToTriples flowDbs = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);
		setupTripleStreamToWriter(flowDbs, wait);

		// Process all morph chains
		Dbs.processDbs(openDbs, Constants.DBS_LOCATION);
		Sigel.processSigel(openSigelDump, Constants.SIGEL_DUMP_LOCATION);
		for (OaiPmhOpener updateOpener : updateOpenerList)
			Sigel.processSigel(updateOpener, Constants.SIGEL_DNB_REPO);
	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(int intervalSize,
			String startOfUpdates, int updateIntervals, CloseSupressor<Triple> wait) {
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
			final StreamToTriples streamToTriplesUpdates =
					Helpers.createTripleStream(true);
			final XmlEntitySplitter xmlSplitter =
					new XmlEntitySplitter(Constants.SIGEL_UPDATE_TOP_LEVEL_TAG,
							Constants.SIGEL_UPDATE_ENTITY);
			final String updateXPath =
					"/" + Constants.SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
							+ Constants.SIGEL_UPDATE_ENTITY + "/" + Constants.SIGEL_XPATH;
			final StreamToTriples flowUpdates = //
					Sigel.setupSigelMorph(openSigelUpdates, xmlSplitter, updateXPath)
							.setReceiver(streamToTriplesUpdates);
			setupTripleStreamToWriter(flowUpdates, wait);
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

	private static int calculateIntervals(String start, String end,
			int intervalSize) {
		final LocalDate startDate = LocalDate.parse(start);
		final LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

	static void setupTripleStreamToWriter(StreamToTriples flow,
			CloseSupressor<Triple> wait) {
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-enriched.xml");
		final TripleRematch rematchTriples = new TripleRematch("isil");
		final TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		final JsonEncoder encodeJson = Helpers.createJsonEncoder(true);
		final ObjectWriter<String> writer =
				new ObjectWriter<>(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH + "enriched.out.json");
		final JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("id", "organisation", "organisations");
		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(rematchTriples)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(writer);
	}
}