/* Copyright 2023, hbz. Licensed under the EPL 2.0 */

package index;

import java.io.IOException;

import controllers.Index;
import org.elasticsearch.ElasticsearchException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class TestBadDocuments  {

	@Test
	public void logIndexFailure()  {
		System.setProperty("config.resource", "test.conf");
		try {
			Index.initialize("test/index/corruptDocument.json");
		} catch (ElasticsearchException | IOException e) {
			Class clazz = e.getClass();
			assertTrue(e.getClass().getName() == "org.elasticsearch.ElasticsearchException");
		}
	}
}
