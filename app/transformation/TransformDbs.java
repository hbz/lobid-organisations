/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import org.metafacture.metafix.Metafix;
import org.metafacture.csv.CsvDecoder;
import org.metafacture.json.JsonEncoder;
import org.metafacture.io.LineReader;
import org.metafacture.triples.StreamToTriples;
import org.metafacture.triples.TripleFilter;
import org.metafacture.triples.TripleCollect;
import org.metafacture.io.ObjectWriter;
import org.metafacture.io.FileOpener;
import org.metafacture.metafix.Metafix;
import java.io.FileNotFoundException;

/**
 * Transformation from DBS CSV to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class TransformDbs {
	static void process(final String outputPath, String geoLookupServer) throws FileNotFoundException {
		final FileOpener opener = new FileOpener();
		StreamToTriples streamToTriples = new StreamToTriples();
		streamToTriples.setRedirect(true);
		opener.setEncoding("UTF-8");
		final CsvDecoder decoder = new CsvDecoder(',');
		decoder.setHasHeader(true);
		final TripleFilter tripleFilter = new TripleFilter();
		tripleFilter.setSubjectPattern(".+"); // Remove entries without id
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		opener//
				.setReceiver(new LineReader())//
				.setReceiver(decoder)//
				.setReceiver(new Metafix("conf/fix-dbs.fix"))//
				.setReceiver(streamToTriples)//
				.setReceiver(tripleFilter)//
				.setReceiver(new TripleCollect())//
				.setReceiver(TransformAll.fixEnriched(geoLookupServer))//
				.setReceiver(encodeJson)//
				.setReceiver(TransformAll.esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		opener.process(TransformAll.DATA_INPUT_DIR + "dbs.csv");
		opener.closeStream();
	}
}
