/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

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
			{ "name", "köln", /*->*/ 2 },
			{ "name", "koeln", /*->*/ 2 },
			{ "name", "koln", /*->*/ 2 },
			{ "alternateName", "köln", /*->*/ 2 },
			{ "alternateName", "koeln", /*->*/ 2 },
			{ "alternateName", "koln", /*->*/ 2 },
			{ "classification.label.de", "Universitätsbibliothek", /*->*/ 9 },
			{ "classification.label.de", "Universitaetsbibliothek", /*->*/ 9 },
			{ "classification.label.de", "Universitatsbibliothek", /*->*/ 9 },
			{ "fundertype.label.de", "Körperschaft", /*->*/ 5 },
			{ "fundertype.label.de", "Koerperschaft", /*->*/ 5 },
			{ "fundertype.label.de", "Korperschaft", /*->*/ 5 },
			{ "_all", "straße", /*->*/ 1 },
			{ "_all", "strasse", /*->*/ 1 },
			{ "name", "Preußischer", /*->*/ 2 },
			{ "name", "Preussischer", /*->*/ 2 },
			{ "name", "preuss", /*->*/ 2 }});
	} // @formatter:on

	private final String field;
	private final String q;
	private final long expectedCount;

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
