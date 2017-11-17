/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package index;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestNGram extends ElasticsearchTest {

	@Test
	public void requestFullTerm() {
		long total = search("name", "Stadtbibliothek").getHits().getTotalHits();
		assertEquals("Request should return correct total hits", 7, total);
	}

	@Test
	public void requestNGram() {
		long total = search("name", "Stadtbib").getHits().getTotalHits();
		assertEquals("Request should return correct total hits", 7, total);
	}

	@Test
	public void requestLowerCase() {
		long total = search("name", "stadtbibliothek").getHits().getTotalHits();
		assertEquals("Request should return correct total hits", 7, total);
	}

	@Test
	public void requestUpperCase() {
		long total = search("name", "STADTBIBLIOTHEK").getHits().getTotalHits();
		assertEquals("Request should return correct total hits", 7, total);
	}
}
