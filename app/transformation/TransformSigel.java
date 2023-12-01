/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import org.metafacture.framework.ObjectReceiver;
import org.metafacture.framework.helpers.DefaultObjectPipe;
import org.metafacture.json.JsonEncoder;
import org.metafacture.metafix.Metafix;
import org.metafacture.biblio.pica.PicaXmlHandler;
import org.metafacture.io.LineReader;
import org.metafacture.biblio.pica.PicaDecoder;
import org.metafacture.xml.XmlDecoder;
import org.metafacture.xml.XmlElementSplitter;
import org.metafacture.io.ObjectWriter;
import org.metafacture.xml.XmlFilenameWriter;
import org.metafacture.io.FileOpener;
import org.metafacture.biblio.OaiPmhOpener;


import controllers.Application;
import play.Logger;

/**
 * Transformation from Sigel PicaPlus-XML to JSON.
 * 
 * @author Fabian Steeg (fsteeg), Tobias Bülte (@TobiasNx)
 *
 */
public class TransformSigel {

	// This opens the pica binary bulk we have, transforms them and saves them as JSON ES Bulk.
	static void processBulk(final String outputPath, final String geoLookupServer, final String wikidataLookupFilename) throws IOException {
		final FileOpener dumpOpener = new FileOpener();	
		PicaDecoder picaDecoder = new PicaDecoder();
		picaDecoder.setNormalizeUTF8(true);
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		dumpOpener//
				.setReceiver(new LineReader())//
				.setReceiver(picaDecoder)//
				.setReceiver(new Metafix("conf/fix-sigel.fix"))//
				.setReceiver(TransformAll.fixEnriched(geoLookupServer, wikidataLookupFilename))//
				.setReceiver(encodeJson)//
				.setReceiver(TransformAll.esBulk())//
				.setReceiver(new ObjectWriter<>(outputPath));
		dumpOpener.process(TransformAll.DATA_INPUT_DIR + "sigel.dat");
 		dumpOpener.closeStream(); 
	}

// This opens the updates and transforms them and appends them to the JSON ES Bulk of the bulk transformation.
		static void processUpdates(String startOfUpdates,
			final String outputPath, final String geoLookupServer, final String wikidataLookupFilename) throws IOException {
		OaiPmhOpener sigelOaiPmhUpdates = new OaiPmhOpener();
		sigelOaiPmhUpdates.setDateFrom(startOfUpdates);
		sigelOaiPmhUpdates.setMetadataPrefix("PicaPlus-xml");
		sigelOaiPmhUpdates.setSetSpec("bib");
		JsonEncoder encodeJson = new JsonEncoder();
		encodeJson.setPrettyPrinting(true);
		ObjectWriter objectWriter = new ObjectWriter<>(outputPath);
		objectWriter.setAppendIfFileExists(true);
		sigelOaiPmhUpdates//			
				.setReceiver(new XmlDecoder())//
				.setReceiver(new PicaXmlHandler())//
				.setReceiver(new Metafix("conf/fix-sigel.fix")) // Preprocess Sigel-Data and fix skips all records that have no "inr" and "isil"
				.setReceiver(TransformAll.fixEnriched(geoLookupServer, wikidataLookupFilename))// Process and enrich Sigel-Data.
				.setReceiver(encodeJson)//
				.setReceiver(TransformAll.esBulk())//
				.setReceiver(objectWriter);
		sigelOaiPmhUpdates.process(Application.CONFIG.getString("transformation.sigel.repository"));
		sigelOaiPmhUpdates.closeStream();
	}
}
