/* Copyright 2014 hbz, Fabian Steeg. Licensed under the Eclipse Public License 1.0 */

package flow;

import java.io.IOException;
import java.util.Map;

import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.ObjectReceiver;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Index JSON in Elasticsearch.
 * 
 * @author Fabian Steeg (fsteeg), Philipp v. Boeselager
 */
@In(String.class)
@Out(Void.class)
public class ElasticsearchIndexer
		extends DefaultObjectPipe<String, ObjectReceiver<Void>> {

	final private ObjectMapper mMapper = new ObjectMapper();
	final private String mIdKey;

	/**
	 * @param aIdKey The key of the JSON value to be used as the ID for the record
	 */
	public ElasticsearchIndexer(String aIdKey) {
		mIdKey = aIdKey;
	}

	@Override
	public void process(String aObj) {
		try {
			String[] recordParts = aObj.split("\n");
			Map<String, Object> json = mMapper.readValue(recordParts[1], Map.class);
			String id = (String) json.get(mIdKey);
			index(json, id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void index(Map<String, Object> aJson, String aId) {
		int retries = 40;
		while (retries > 0) {
			try {
				Constants.ES_CLIENT
						.prepareIndex(Constants.ES_INDEX,
								Constants.ES_TYPE, aId)
						.setSource(aJson).execute().actionGet();
				break; // stop retry-while
			} catch (NoNodeAvailableException e) {
				retries--;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException x) {
					x.printStackTrace();
				}
				System.err.printf("Retry indexing record %s: %s (%s more retries)\n",
						aId, e.getMessage(), retries);
			}
		}
	}
}
