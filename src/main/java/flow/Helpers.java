package flow;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.source.OaiPmhOpener;

/**
 * @author pvb
 */
public class Helpers {

	/**
	 * @param isRedirected define whether stream shall be redirected or not
	 * @return a new StreamToTriples
	 */
	public static StreamToTriples createTripleStream(boolean isRedirected) {
		StreamToTriples streamToTriples = new StreamToTriples();
		streamToTriples.setRedirect(isRedirected);
		return streamToTriples;
	}

	/**
	 * @param isPrettyPrinting define whether encoder shall be pretty printing or
	 *          not
	 * @return a new JsonEncoder
	 */
	public static JsonEncoder createJsonEncoder(boolean isPrettyPrinting) {
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(isPrettyPrinting);
		return encodeJson;
	}

	/**
	 * @param start the start of updates formatted in yyyy-MM-dd
	 * @param end the end of updates formatted in yyyy-MM-dd
	 * @return a new OaiPmhOpener
	 */
	public static OaiPmhOpener createOaiPmhOpener(String start, String end) {
		OaiPmhOpener opener = new OaiPmhOpener();
		opener.setDateFrom(start);
		opener.setDateUntil(end);
		opener.setMetadataPrefix("PicaPlus-xml");
		opener.setSetSpec("bib");
		return opener;
	}

	/**
	 * @return a String representing today in yyyy-MM-dd format
	 */
	public static String getToday() {
		String dateFormat = "yyyy-MM-dd";
		Calendar calender = Calendar.getInstance();
		SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);
		return simpleDate.format(calender.getTime());
	}

}
