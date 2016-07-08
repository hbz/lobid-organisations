package transformation;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import play.Logger;
import play.libs.Json;

/**
 * Export organisations JSON data as CSV.
 * 
 * @author Fabian Steeg (fsteeg)
 */
public class CsvExport {

	private JsonNode organisations;

	/**
	 * @param json The organisations JSON data to export
	 */
	public CsvExport(String json) {
		this.organisations = Json.parse(json);
	}

	/**
	 * @param fields The JSON fields to include in the export
	 * @return The data for the given fields in CSV format
	 */
	public String of(List<String> fields) {
		String csv = fields.stream().collect(Collectors.joining(",")) + "\n";
		for (Iterator<JsonNode> iter = organisations.elements(); iter.hasNext();) {
			JsonNode org = iter.next();
			csv += fields.stream().map(field -> {
				try {
					String value = JsonPath.read(Configuration.defaultConfiguration()
							.jsonProvider().parse(org.toString()), "$." + field);
					return String.format("\"%s\"", value);
				} catch (PathNotFoundException x) {
					Logger.warn(x.getMessage());
					// https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#empty-and-quoted-cells
					return "";
				}
			}).collect(Collectors.joining(",")) + "\n";
		}
		return csv;
	}

}
