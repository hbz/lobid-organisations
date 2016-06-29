/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package index;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestCollection extends ElasticsearchTest {

	@Test
	public void searchForCollection() {
		long total = search("type", "Collection").getHits().getTotalHits();
		assertEquals("Request should return 0", 0, total);
	}

}
