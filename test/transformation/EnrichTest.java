/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.culturegraph.mf.stream.converter.StreamToTriples;
import org.culturegraph.mf.stream.pipe.CloseSupressor;
import org.culturegraph.mf.stream.pipe.XmlElementSplitter;
import org.culturegraph.mf.stream.pipe.sort.TripleSort;
import org.culturegraph.mf.stream.source.FileOpener;
import org.culturegraph.mf.types.Triple;
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
public class EnrichTest {

	static {
		System.setProperty("config.resource", "test.conf");
		System.out.println("Using CONFIG from " + Application.CONFIG.origin());
	}

	private static final CloseSupressor<Triple> WAIT = new CloseSupressor<>(2);
	private static final String SIGEL_DUMP_LOCATION =
			Enrich.DATA_INPUT_DIR + "sigel.xml";
	private static final String DBS_LOCATION = Enrich.DATA_INPUT_DIR + "dbs.csv";
	private static final String DUMP_XPATH =
			"/" + Enrich.SIGEL_DUMP_TOP_LEVEL_TAG + "/" + Enrich.SIGEL_XPATH;

	@BeforeClass
	public static void setUp() {
		File output = new File(Enrich.DATA_OUTPUT_FILE);
		assertThat(!output.exists() || output.delete()).as("no output file")
				.isTrue();
	}

	@AfterClass
	public static void tearDown() {
		File output = new File(Enrich.DATA_OUTPUT_FILE);
		assertThat(output.length()).as("output file size").isGreaterThan(0);
	}

	@Test
	public void sigelMorph() throws IOException {
		final FileOpener splitFileOpener = new FileOpener();
		StreamToTriples sigelFlow = Sigel.setupSigelMorph(splitFileOpener)
				.setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(sigelFlow, WAIT, new TripleSort(),
				new TripleRematch("isil"), Enrich.DATA_OUTPUT_FILE, null);
		Sigel.processSigelMorph(splitFileOpener, Enrich.DATA_OUTPUT_DIR);
	}

	@Test
	public void dbsTransformation() {
		final FileOpener openDbs = new FileOpener();
		final StreamToTriples dbsFlow = //
				Dbs.morphDbs(openDbs).setReceiver(Helpers.createTripleStream(true));
		Helpers.setupTripleStreamToWriter(dbsFlow, WAIT, new TripleSort(),
				new TripleRematch("isil"), Enrich.DATA_OUTPUT_FILE, null);
		Dbs.processDbs(openDbs, DBS_LOCATION);
	}

	@Test
	public void multiLangAlternateName() throws IOException {
		Enrich.process("", 0, Enrich.DATA_OUTPUT_FILE, "");
		assertThat(
				new String(Files.readAllBytes(Paths.get(Enrich.DATA_OUTPUT_FILE))))
						.as("transformation output with multiLangAlternateName")
						.contains("Leibniz Institute").contains("Berlin SBB");
	}

	@Test
	public void sigelSplitting() {
		final FileOpener sourceFileOpener = new FileOpener();
		final XmlElementSplitter xmlSplitter = new XmlElementSplitter(
				Enrich.SIGEL_DUMP_TOP_LEVEL_TAG, Enrich.SIGEL_DUMP_ENTITY);
		Sigel.setupSigelSplitting(sourceFileOpener, xmlSplitter, DUMP_XPATH,
				Enrich.DATA_OUTPUT_DIR);
		Sigel.processSigelSplitting(sourceFileOpener, SIGEL_DUMP_LOCATION);
	}

}
