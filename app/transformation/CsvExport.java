/* Copyright 2014-2017, hbz. Licensed under the EPL 2.0 */

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
 * Export organisations JSON data as CSV. Allows defining an other
 * separator than comma.
 * 
 * @author Fabian Steeg (fsteeg)
 */
public class CsvExport {

	private final JsonNode organisations;
	public final static String DEFAULT_SEPARATOR = ",";
	public final static String TAB_SEPARATOR = "\t";

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
		return of(fields, DEFAULT_SEPARATOR);
	}

	/**
	 * @param fields The JSON fields to include in the export
	 * @param separator The separator to separate entries in the CSV
	 * @return The data for the given fields in [C*]SV format
	 */
	public String of(final String fields, final String separator) {
		StringBuilder csv = new StringBuilder(fields + "\n");
		for (Iterator<JsonNode> iter = organisations.elements(); iter.hasNext(); ) {
			JsonNode org = iter.next();
			csv.append(Arrays.asList(fields.split(separator)).stream().map(field -> {
				try {
					Object value = JsonPath.read(Configuration.defaultConfiguration()
							.jsonProvider().parse(org.toString()), "$." + field);
					return separator==DEFAULT_SEPARATOR ? String.format("\"%s\"",
							value.toString().replaceAll("\"", "\"\"")) : value.toString();
				}
				catch (PathNotFoundException x) {
					Logger.trace(x.getMessage());
					// https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#empty-and-quoted-cells
					return "";
				}
			}).collect(Collectors.joining(separator))).append("\n");
		}
		return csv.toString();
	}
}
