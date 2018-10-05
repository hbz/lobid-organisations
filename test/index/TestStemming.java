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
			{ "name", "köln", /*->*/ 1 },
			{ "name", "koeln", /*->*/ 1 },
			{ "name", "koln", /*->*/ 1 },
			{ "alternateName", "köln", /*->*/ 2 },
			{ "alternateName", "koeln", /*->*/ 2 },
			{ "alternateName", "koln", /*->*/ 2 },
			{ "classification.label.de", "Universitätsbibliothek", /*->*/ 8 },
			{ "classification.label.de", "Universitaetsbibliothek", /*->*/ 8 },
			{ "classification.label.de", "Universitatsbibliothek", /*->*/ 8 },
			{ "fundertype.label.de", "Körperschaft", /*->*/ 4 },
			{ "fundertype.label.de", "Koerperschaft", /*->*/ 4 },
			{ "fundertype.label.de", "Korperschaft", /*->*/ 4 },
			{ "_all", "straße", /*->*/ 1 },
			{ "_all", "strasse", /*->*/ 1 },
			{ "name", "Preußischer", /*->*/ 2 },
			{ "name", "Preussischer", /*->*/ 2 },
			{ "name", "preuss", /*->*/ 2 }});
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
