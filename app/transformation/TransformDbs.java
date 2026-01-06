/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import org.metafacture.metafix.Metafix;
import org.metafacture.csv.CsvDecoder;
import org.metafacture.json.JsonEncoder;
import org.metafacture.io.LineReader;
import org.metafacture.io.ObjectWriter;
import org.metafacture.strings.StringMatcher;
import org.metafacture.io.FileOpener;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Transformation from DBS CSV to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class TransformDbs {
	static void process(final String outputPath, final String wikidataLookupFilename) throws IOException, FileNotFoundException {
		final FileOpener opener = new FileOpener();
		opener.setEncoding("UTF-8");
		final StringMatcher matcher = new StringMatcher();
		matcher.setPattern("NULL");
		matcher.setReplacement("");
		final CsvDecoder decoder = new CsvDecoder(',');
		decoder.setHasHeader(true);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		opener//
				.setReceiver(new LineReader())//
				.setReceiver(matcher)//
				.setReceiver(decoder)//
				.setReceiver(new Metafix("conf/fix-dbs.fix"))// Fix skips all records that have no "inr"
				.setReceiver(TransformAll.fixEnriched(wikidataLookupFilename))//
				.setReceiver(encodeJson)//
				.setReceiver(TransformAll.esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		opener.process(TransformAll.DATA_INPUT_DIR + "dbs.csv");
		opener.closeStream();
	}
}
