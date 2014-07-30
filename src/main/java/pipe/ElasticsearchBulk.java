package pipe;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.ObjectReceiver;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;
import org.culturegraph.mf.stream.converter.JsonEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Add Elasticsearch bulk indexing metadata to JSON input.<br/>
 * Use after {@link JsonEncoder}, before writing.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
@In(String.class)
@Out(String.class)
public class ElasticsearchBulk extends
		DefaultObjectPipe<String, ObjectReceiver<String>> {

	private ObjectMapper mapper = new ObjectMapper();
	private String idKey;
	private String type;
	private String index;

	/**
	 * @param idKey The key of the JSON value to be used as the ID for the record
	 * @param type The Elasticsearch index type
	 * @param index The Elasticsearch index name
	 */
	public ElasticsearchBulk(String idKey, String type, String index) {
		this.idKey = idKey;
		this.type = type;
		this.index = index;
	}

	@Override
	public void process(String obj) {
		StringWriter stringWriter = new StringWriter();
		try {
			Map<String, Object> json = mapper.readValue(obj, Map.class);
			Map<String, Object> detailsMap = new HashMap<>();
			Map<String, Object> indexMap = new HashMap<>();
			indexMap.put("index", detailsMap);
			detailsMap.put("_id", json.get(idKey));
			detailsMap.put("_type", type);
			detailsMap.put("_index", index);
			mapper.writeValue(stringWriter, indexMap);
			stringWriter.write("\n");
			mapper.writeValue(stringWriter, json);
		} catch (IOException e) {
			e.printStackTrace();
		}
		getReceiver().process(stringWriter.toString());
	}
}
