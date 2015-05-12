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

	static final String SIGEL_DUMP_LOCATION = Constants.MAIN_RESOURCES_PATH
			+ Constants.INPUT_PATH + "sigel.xml";
	static final String SIGEL_DNB_REPO =
			"http://gnd-proxy.lobid.org/oai/repository";
	static final String DBS_LOCATION = Constants.MAIN_RESOURCES_PATH
			+ Constants.INPUT_PATH + "/dbs.csv";

	/**
	 * @param args start date of Sigel updates (date of Sigel base dump) and size
	 *          of update intervals in days
	 */
	public static void main(String... args) {
		String startOfUpdates = args[0];
		int intervalSize = Integer.parseInt(args[1]);
		process(startOfUpdates, intervalSize);
	}

	static void process(String startOfUpdates, int intervalSize) {
		int updateIntervals =
				calculateIntervals(startOfUpdates, Sigel.getToday(), intervalSize);
		final CloseSupressor<Triple> wait =
				new CloseSupressor<>(updateIntervals + 2);

		// Sigel Dump
		final FileOpener openSigelDump = new FileOpener();
		final StreamToTriples streamToTriplesDump = new StreamToTriples();
		streamToTriplesDump.setRedirect(true);
		final String dumpXPath =
				"/" + Constants.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Constants.SIGEL_XPATH;
		final XmlEntitySplitter xmlSplitter =
				new XmlEntitySplitter(Constants.SIGEL_DUMP_TOP_LEVEL_TAG,
						Constants.SIGEL_DUMP_ENTITY);
		StreamToTriples flowSigelDump = //
				Sigel.morphSigel(openSigelDump, xmlSplitter, dumpXPath).setReceiver(
						streamToTriplesDump);
		continueWith(flowSigelDump, wait);

		// Sigel Update
		ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, startOfUpdates, updateIntervals, wait);

		// Dbs
		FileOpener openDbs = new FileOpener();
		StreamToTriples streamToTriplesDbs = new StreamToTriples();
		streamToTriplesDbs.setRedirect(true);
		StreamToTriples flowDbs = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);
		continueWith(flowDbs, wait);

		Sigel.processSigel(openSigelDump, SIGEL_DUMP_LOCATION);
		for (OaiPmhOpener updateOpener : updateOpenerList)
			Sigel.processSigel(updateOpener, SIGEL_DNB_REPO);
		Dbs.processDbs(openDbs, DBS_LOCATION);
	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(int intervalSize,
			String startOfUpdates, int updateIntervals, CloseSupressor<Triple> wait) {
		String start = startOfUpdates;
		ArrayList<OaiPmhOpener> updateOpenerList = new ArrayList<>();
		String end = addDays(start, intervalSize);

		// There has to be at least one interval
		int intervals;
		if (updateIntervals == 0)
			intervals = 1;
		else
			intervals = updateIntervals;

		for (int i = 0; i < intervals; i++) {
			OaiPmhOpener openSigelUpdates = Sigel.createOaiPmhOpener(start, end);
			StreamToTriples streamToTriplesUpdates = new StreamToTriples();
			streamToTriplesUpdates.setRedirect(true);
			final XmlEntitySplitter xmlSplitter =
					new XmlEntitySplitter(Constants.SIGEL_UPDATE_TOP_LEVEL_TAG,
							Constants.SIGEL_UPDATE_ENTITY);
			final String updateXPath =
					"/" + Constants.SIGEL_UPDATE_TOP_LEVEL_TAG + "/"
							+ Constants.SIGEL_UPDATE_ENTITY + "/" + Constants.SIGEL_XPATH;
			StreamToTriples flowUpdates = //
					Sigel.morphSigel(openSigelUpdates, xmlSplitter, updateXPath)
							.setReceiver(streamToTriplesUpdates);
			continueWith(flowUpdates, wait);
			updateOpenerList.add(openSigelUpdates);
			start = addDays(start, intervalSize);
			if (i == intervals - 2)
				end = Sigel.getToday();
			else
				end = addDays(end, intervalSize);
		}
		return updateOpenerList;
	}

	private static String addDays(String start, int intervalSize) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String result = null;
		try {
			Date startDate = dateFormat.parse(start);
			Calendar calender = Calendar.getInstance();
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
		LocalDate startDate = LocalDate.parse(start);
		LocalDate endDate = LocalDate.parse(end);
		long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

	static void continueWith(StreamToTriples flow, CloseSupressor<Triple> wait) {
		TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-enriched.xml");
		TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter<String> writer =
				new ObjectWriter<>(Constants.MAIN_RESOURCES_PATH
						+ Constants.OUTPUT_PATH + "enriched.out.json");
		JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("id", "organisation", "organisations");
		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(writer);
	}
}