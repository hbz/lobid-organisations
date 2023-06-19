/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package transformation;

import org.metafacture.metamorph.test.MetamorphTestSuite;
import org.junit.runner.RunWith;

@SuppressWarnings("javadoc")
@RunWith(MetamorphTestSuite.class)
@MetamorphTestSuite.TestDefinitions({
		//
		"/test_morph-dbs.xml", //
		"/test_morph-sigel.xml", //
		"/test_morph-enriched.xml" })
public final class TestMorphs {
	/* bind to xml tests */
}