/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

package transformation;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.metafacture.framework.helpers.DefaultObjectReceiver;
import org.metafacture.metamorph.Metamorph;
import org.metafacture.metafix.Metafix;
import org.metafacture.formeta.FormetaEncoder;
import org.metafacture.biblio.pica.PicaXmlHandler;
import org.metafacture.xml.XmlDecoder;
import org.metafacture.xml.XmlElementSplitter;
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
			TransformAll.DATA_INPUT_DIR + "sigel.xml";
	private static final String DUMP_XPATH =
			"/" + TransformSigel.DUMP_TOP_LEVEL_TAG + "/" + TransformSigel.XPATH;

	@BeforeClass
	public static void setUp() {
		File output = new File(TransformAll.DATA_OUTPUT_FILE);
		assertThat(!output.exists() || output.delete()).as("no output file")
				.isTrue();
	}

	@AfterClass
	public static void tearDown() {
		File output = new File(TransformAll.DATA_OUTPUT_FILE);
		assertThat(output.length()).as("output file size").isGreaterThan(0);
	}

	@Test
	public void multiLangAlternateName() throws IOException {
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "");
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with multiLangAlternateName")
						.contains("Leibniz Institute").contains("Berlin SBB");
	}

	@Test
	public void separateUrlAndProvidesFields() throws IOException {
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "");
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with `url` and `provides`")
						.contains("http://www.medpilot.de/?idb=ZBMED")
						.contains("http://www.zbmed.de");
	}

	@Test
	public void preferSigelData() throws IOException {
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "");
		assertThat(new String(
				Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with preferred Sigel data")
						.contains("Hauptabteilung")//
						.as("transformation output containing DBS data")
						.contains("Roemer-Museum");
	}

	@Test
	public void sigelSplitting() {
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				TransformSigel.DUMP_TOP_LEVEL_TAG, TransformSigel.DUMP_ENTITY);
		TransformSigel.setupSigelSplitting(sourceFileOpener, xmlSplitter,
				DUMP_XPATH, TransformAll.DATA_OUTPUT_DIR);
		sourceFileOpener.process(SIGEL_DUMP_LOCATION);
		sourceFileOpener.closeStream();
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
		sourceFileOpener.setReceiver(new XmlDecoder())
				.setReceiver(new PicaXmlHandler())//
				.setReceiver(new Metafix("conf/fix-sigel.fix"))//
				.setReceiver(new Metamorph("morph-enriched.xml"))//
				.setReceiver(encoder);
		sourceFileOpener.process(SIGEL_DUMP_LOCATION);
		sourceFileOpener.closeStream();
		assertThat(resultCollector.toString())//
				.as("contains api description")//
				.contains(
						"availableChannel[]{{serviceType:SRU,type[]{type:ServiceChannel,type:WebAPI}serviceUrl:http\\://info-test.de/sru}"//
								+ "{serviceType:other,type[]{type:ServiceChannel}serviceUrl:http\\://info-test.de/other}"//
								+ "{serviceType:OpenURL,type[]{type:ServiceChannel,type:WebAPI}serviceUrl:http\\://info-test.de/openurl}"//
								+ "{serviceType:PAIA,type[]{type:ServiceChannel,type:WebAPI}serviceUrl:http\\://info-test.de/paia}"//
								+ "{serviceType:DAIA,type[]{type:ServiceChannel,type:WebAPI}serviceUrl:http\\://info-test.de/daia}}");
	}

}
