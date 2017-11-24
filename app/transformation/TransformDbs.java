/* Copyright 2014-2017, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.CsvDecoder;
import org.culturegraph.mf.stream.converter.JsonEncoder;
import org.culturegraph.mf.stream.converter.LineReader;
import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.TripleFilter;
import org.culturegraph.mf.stream.pipe.sort.TripleCollect;
import org.culturegraph.mf.stream.sink.ObjectWriter;
import org.culturegraph.mf.stream.source.FileOpener;

/**
 * Transformation from DBS CSV to JSON.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class TransformDbs {
	static void process(final String outputPath, String geoLookupServer) {
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
				.setReceiver(new Metamorph("morph-dbs.xml"))//
				.setReceiver(streamToTriples)//
				.setReceiver(tripleFilter)//
				.setReceiver(new TripleCollect())//
				.setReceiver(TransformAll.morphEnriched(geoLookupServer))//
				.setReceiver(encodeJson)//
				.setReceiver(TransformAll.esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		opener.process(TransformAll.DATA_INPUT_DIR + "dbs.csv");
		opener.closeStream();
	}
}
