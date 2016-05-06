import static org.junit.Assert.assertEquals;

import org.elasticsearch.action.search.SearchResponse;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestJsonStructure extends ElasticsearchTest {

	@Test
	public void checkHashBangInIDs() {
		final SearchResponse sr1 =
				exactSearch("id", "http://beta.lobid.org/organisations/DE-38#!");
		assertEquals("Request should return 1", 1, sr1.getHits().getTotalHits());
		final SearchResponse sr2 =
				exactSearch("id", "http://beta.lobid.org/organisations/DE-294#!");
		assertEquals("Request should return 1", 1, sr2.getHits().getTotalHits());
		final SearchResponse sr3 =
				exactSearch("id", "http://beta.lobid.org/organisations/DE-1a#!");
		assertEquals("Request should return 1", 1, sr3.getHits().getTotalHits());
	}
}
