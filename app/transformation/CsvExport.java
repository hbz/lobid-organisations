package transformation;

import java.util.Arrays;
import java.util.Iterator;
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
	public String of(String fields) {
		String csv = fields + "\n";
		for (Iterator<JsonNode> iter = organisations.elements(); iter.hasNext();) {
			JsonNode org = iter.next();
			csv += Arrays.asList(fields.split(",")).stream().map(field -> {
				try {
					Object value = JsonPath.read(Configuration.defaultConfiguration()
							.jsonProvider().parse(org.toString()), "$." + field);
					return String.format("\"%s\"",
							value.toString().replaceAll("\"", "\"\""));
				} catch (PathNotFoundException x) {
					Logger.trace(x.getMessage());
					// https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#empty-and-quoted-cells
					return "";
				}
			}).collect(Collectors.joining(",")) + "\n";
		}
		return csv;
	}

}
