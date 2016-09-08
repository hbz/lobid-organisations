/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package transformation;

import org.culturegraph.mf.test.MetamorphTestSuite;
import org.culturegraph.mf.test.MetamorphTestSuite.TestDefinitions;
import org.junit.runner.RunWith;

@SuppressWarnings("javadoc")
@RunWith(MetamorphTestSuite.class)
@TestDefinitions({
		//
		"/test_morph-dbs.xml", //
		"/test_morph-sigel.xml", //
		"/test_morph-enriched.xml" })
public final class TestMorphs {
	/* bind to xml tests */
}