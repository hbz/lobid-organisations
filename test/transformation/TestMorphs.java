package transformation;

import org.culturegraph.mf.test.TestSuite;
import org.culturegraph.mf.test.TestSuite.TestDefinitions;
import org.junit.runner.RunWith;

@SuppressWarnings("javadoc")
@RunWith(TestSuite.class)
@TestDefinitions({
		//
		"/test_morph-dbs.xml", //
		"/test_morph-sigel.xml", //
		"/test_morph-enriched.xml" })
public final class TestMorphs {
	/* bind to xml tests */
}