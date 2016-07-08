package transformation;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Export organisations JSON data as CSV.
 * 
 * @author Fabian Steeg (fsteeg)
 */
public class CsvExport {

	private List<ObjectNode> organisations;

	/**
	 * @param organisations The organisations JSON data to export
	 */
	public CsvExport(List<ObjectNode> organisations) {
		this.organisations = organisations;
	}

	/**
	 * @param fields The JSON fields to include in the export
	 * @return The data for the given fields in CSV format
	 */
	public String of(List<String> fields) {
		String csv = fields.stream().collect(Collectors.joining(",")) + "\n";
		for (ObjectNode org : organisations) {
			csv += fields.stream().map(field -> {
				try {
					String value = JsonPath.read(Configuration.defaultConfiguration()
							.jsonProvider().parse(org.toString()), "$." + field);
					return String.format("\"%s\"", value);
				} catch (PathNotFoundException x) {
					throw new IllegalArgumentException(x.getMessage());
				}
			}).collect(Collectors.joining(",")) + "\n";
		}
		return csv;
	}

}
