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
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.stream.source.OaiPmhOpener;
import org.culturegraph.mf.types.Triple;

@SuppressWarnings("javadoc")
public class EnrichToElasticsearch {

	/**
	 * @param args start date of Sigel updates (date of Sigel base dump) and size
	 *          of update intervals in days
	 */
	public static void main(final String... args) {
		String startOfUpdates = args[0];
		int intervalSize = Integer.parseInt(args[1]);
		process(startOfUpdates, intervalSize, Constants.MAIN_RESOURCES_PATH);
	}

	static void process(final String startOfUpdates, final int intervalSize,
			final String aResourcesPath) {
		final String start = startOfUpdates;
		int updateIntervals =
				calculateIntervals(start, Helpers.getToday(), intervalSize);
		final CloseSupressor<Triple> wait =
				new CloseSupressor<>(updateIntervals + 2);

		final FileOpener openSigelDump = new FileOpener();
		final StreamToTriples streamToTriplesDump = new StreamToTriples();
		streamToTriplesDump.setRedirect(true);
		final StreamToTriples flowSigelDump = null; // TODO:
																								// Sigel.morphSigel(openSigelDump).setReceiver(streamToTriplesDump);
		continueWith(flowSigelDump, wait);

		final ArrayList<OaiPmhOpener> updateOpenerList =
				buildUpdatePipes(intervalSize, start, updateIntervals, wait);

		final FileOpener openDbs = new FileOpener();
		final StreamToTriples streamToTriplesDbs = new StreamToTriples();
		streamToTriplesDbs.setRedirect(true);
		final StreamToTriples flowDbs = //
				Dbs.morphDbs(openDbs).setReceiver(streamToTriplesDbs);
		continueWith(flowDbs, wait);

		Sigel.processSigel(openSigelDump, aResourcesPath
				+ Constants.SIGEL_DUMP_LOCATION);
		for (OaiPmhOpener updateOpener : updateOpenerList)
			Sigel.processSigel(updateOpener, Constants.SIGEL_DNB_REPO);
		Dbs.processDbs(openDbs, Constants.DBS_LOCATION);
	}

	private static ArrayList<OaiPmhOpener> buildUpdatePipes(
			final int intervalSize, final String startOfUpdates,
			final int updateIntervals, final CloseSupressor<Triple> wait) {
		String start = startOfUpdates;
		final ArrayList<OaiPmhOpener> updateOpenerList = new ArrayList<>();
		String end = addDays(start, intervalSize);

		// There has to be at least one interval
		final int intervals;
		if (updateIntervals == 0)
			intervals = 1;
		else
			intervals = updateIntervals;

		for (int i = 0; i < intervals; i++) {
			final OaiPmhOpener openSigelUpdates =
					Helpers.createOaiPmhOpener(start, end);
			final StreamToTriples streamToTriplesUpdates = new StreamToTriples();
			streamToTriplesUpdates.setRedirect(true);
			final StreamToTriples flowUpdates = null; // TODO:
																								// Sigel.morphSigel(openSigelUpdates).setReceiver(streamToTriplesUpdates);
			continueWith(flowUpdates, wait);
			updateOpenerList.add(openSigelUpdates);
			start = addDays(start, intervalSize);
			if (i == intervals - 2)
				end = Helpers.getToday();
			else
				end = addDays(end, intervalSize);
		}

		return updateOpenerList;
	}

	private static String addDays(final String start, final int intervalSize) {
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

	private static int calculateIntervals(final String start, final String end,
			final int intervalSize) {
		final LocalDate startDate = LocalDate.parse(start);
		final LocalDate endDate = LocalDate.parse(end);
		final long timeSpan = startDate.until(endDate, ChronoUnit.DAYS);
		return (int) timeSpan / intervalSize;
	}

	static void continueWith(final StreamToTriples flow,
			final CloseSupressor<Triple> wait) {
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		final Metamorph morph =
				new Metamorph(Constants.MAIN_RESOURCES_PATH + "morph-enriched.xml");
		final TripleSort sortTriples = new TripleSort();
		sortTriples.setBy(Compare.SUBJECT);
		final JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		final JsonToElasticsearchBulk esBulk =
				new JsonToElasticsearchBulk("id", Constants.ES_TYPE, Constants.ES_INDEX);
		final ElasticsearchIndexer elIndex = new ElasticsearchIndexer("id");

		flow.setReceiver(wait)//
				.setReceiver(tripleFilter)//
				.setReceiver(sortTriples)//
				.setReceiver(new TripleCollect())//
				.setReceiver(morph)//
				.setReceiver(encodeJson)//
				.setReceiver(esBulk)//
				.setReceiver(elIndex);

	}
}
