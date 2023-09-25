/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.metafacture.framework.helpers.DefaultObjectReceiver;
import org.metafacture.metamorph.Metamorph;
import org.metafacture.metafix.Metafix;
import org.metafacture.formeta.FormetaEncoder;
import org.metafacture.io.LineReader;
import org.metafacture.biblio.pica.PicaDecoder;
import org.metafacture.io.FileOpener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import controllers.Application;

/**
 * For tests: sample data only, no updates.
 * 
 * @author Simon Ritter (SBRitter), Fabian Steeg (fsteeg), Pascal Christoph
 *         (dr0i)
 */
@SuppressWarnings("javadoc")
public class TestTransformAll {

	static {
		System.setProperty("config.resource", "test.conf");
		System.out.println("Using CONFIG from " + Application.CONFIG.origin());
	}

	private static final String SIGEL_DUMP_LOCATION =
			TransformAll.DATA_INPUT_DIR + "sigel.dat";
	private static final String DUMP_XPATH =
			"/" + TransformSigel.DUMP_TOP_LEVEL_TAG + "/" + TransformSigel.XPATH;
	private static final String wikidataLookupFilename = "../test/conf/wikidataLookup.tsv";

	@BeforeClass
	public static void setUp() throws IOException {
		File output = new File(TransformAll.DATA_OUTPUT_FILE);
		assertThat(!output.exists() || output.delete()).as("no output file")
				.isTrue();
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "", wikidataLookupFilename);
	}

	@AfterClass
	public static void tearDown() {
		File output = new File(TransformAll.DATA_OUTPUT_FILE);
		assertThat(output.length()).as("output file size").isGreaterThan(0);
	}


	@Test
	public void multiLangAlternateName() throws IOException {
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with multiLangAlternateName")
						.contains("Leibniz Institute").contains("Berlin SBB");
	}

	@Test
	public void separateUrlAndProvidesFields() throws IOException {
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with `url` and `provides`")
						.contains("https://www.livivo.de/?idb=ZBMED")
						.contains("https://www.zbmed.de");
	}

	@Test
	public void preferSigelData() throws IOException {
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with preferred Sigel data")
						.contains("Hauptabteilung")//
						.as("transformation output containing DBS data")
						.contains("Roemer-Museum");
	}

	@Test
	public void testContainsApiDescription() throws FileNotFoundException {
		FormetaEncoder encoder = new FormetaEncoder();
		StringBuilder resultCollector = new StringBuilder();
		encoder.setReceiver(new DefaultObjectReceiver<String>() {
			@Override
			public void process(String obj) {
				resultCollector.append(obj);
			}
		});
		final FileOpener sourceFileOpener = new FileOpener();
		PicaDecoder picaDecoder = new PicaDecoder();
		picaDecoder.setNormalizeUTF8(true);
		final HashMap<String, String> fixVariables = new HashMap<>();
		final String wikidataLookupFile ="../test/conf/wikidataLookup.tsv";
		fixVariables.put("isil2wikidata", wikidataLookupFile);
		fixVariables.put("dbsId2wikidata", wikidataLookupFile);
		fixVariables.put("wikidata2gndIdentifier", wikidataLookupFile);
		Metafix fixEnriched = new Metafix("conf/fix-enriched.fix", fixVariables);
		sourceFileOpener.setReceiver(new LineReader())//
				.setReceiver(picaDecoder)//
				.setReceiver(new Metafix("conf/fix-sigel.fix"))//
				.setReceiver(fixEnriched)//
				.setReceiver(encoder);
		sourceFileOpener.process(SIGEL_DUMP_LOCATION);
		sourceFileOpener.closeStream();
		assertThat(resultCollector.toString())//
				.as("contains api description")//
				.contains(
						"availableChannel[]{1{type[]{1:ServiceChannel,2:WebAPI}serviceType:SRU,serviceUrl:http\\://sru.gbv.de/opac-de-hil2}"//
								+ "2{type[]{1:ServiceChannel,2:WebAPI}serviceType:PAIA,serviceUrl:https\\://paia.gbv.de/DE-Hil2/}"//
								+ "3{type[]{1:ServiceChannel,2:WebAPI}serviceType:DAIA,serviceUrl:https\\://paia.gbv.de/DE-Hil2/daia}}")
								;
	}




}