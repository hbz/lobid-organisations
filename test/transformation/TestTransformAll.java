/* Copyright 2014-2017, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.source.FileOpener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import controllers.Application;

/**
 * For tests: sample data only, no updates.
 * 
 * @author Simon Ritter (SBRitter), Fabian Steeg (fsteeg)
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
		assertThat(
				new String(Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with multiLangAlternateName")
						.contains("Leibniz Institute").contains("Berlin SBB");
	}

	@Test
	public void separateUrlAndProvidesFields() throws IOException {
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "");
		assertThat(
				new String(Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with `url` and `provides`")
						.contains("http://www.medpilot.de/?idb=ZBMED")
						.contains("http://www.zbmed.de");
	}

	@Test
	public void preferSigelData() throws IOException {
		TransformAll.process("", 0, TransformAll.DATA_OUTPUT_FILE, "");
		assertThat(
				new String(Files.readAllBytes(Paths.get(TransformAll.DATA_OUTPUT_FILE))))
						.as("transformation output with preferred Sigel data")
						.contains("Hauptabteilung")//
						.as("transformation output containing DBS data")
						.contains("Grundschule");
	}

	@Test
	public void sigelSplitting() {
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				TransformSigel.DUMP_TOP_LEVEL_TAG, TransformSigel.DUMP_ENTITY);
		TransformSigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH,
				TransformAll.DATA_OUTPUT_DIR);
		sourceFileOpener.process(SIGEL_DUMP_LOCATION);
		sourceFileOpener.closeStream();
	}

}
