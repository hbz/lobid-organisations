/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package index;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests to verify behavior of Snowball `German2` stemming, see
 * http://snowball.tartarus.org/algorithms/german2/stemmer.html
 * 
 * @author Fabian Steeg (fsteeg)
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class TestStemming extends ElasticsearchTest {

	// test data parameters, formatted as "input /*->*/ expected output"
	@Parameters
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ "_all", "köln", /*->*/ 2 },
			{ "_all", "koeln", /*->*/ 2 },
			{ "_all", "koln", /*->*/ 2 },
			{ "name", "köln", /*->*/ 1 },
			{ "name", "koeln", /*->*/ 1 },
			{ "name", "koln", /*->*/ 1 },
			{ "alternateName", "köln", /*->*/ 1 },
			{ "alternateName", "koeln", /*->*/ 1 },
			{ "alternateName", "koln", /*->*/ 1 },
			{ "classification.label", "Universitätsbibliothek", /*->*/ 2 },
			{ "classification.label", "Universitaetsbibliothek", /*->*/ 2 },
			{ "classification.label", "Universitatsbibliothek", /*->*/ 2 },
			{ "fundertype.label", "Körperschaft", /*->*/ 2 },
			{ "fundertype.label", "Koerperschaft", /*->*/ 2 },
			{ "fundertype.label", "Korperschaft", /*->*/ 2 },
			{ "_all", "straße", /*->*/ 2 },
			{ "_all", "strasse", /*->*/ 2 },
			{ "name", "Preußischer", /*->*/ 1 },
			{ "name", "Preussischer", /*->*/ 1 },
			{ "name", "preuss", /*->*/ 1 }});
	} // @formatter:on

	private String field;
	private String q;
	private long expectedCount;

	public TestStemming(String field, String q, int count) {
		this.field = field;
		this.q = q;
		this.expectedCount = count;
	}

	@Test
	public void test() {
		long total = search(field, q).getHits().getTotalHits();
		assertThat(total).as(String.format("Hit count for %s:%s", field, q))
				.isEqualTo(expectedCount);
	}

}
