package conversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

/**
 * Simple class to convert an address from json input to csv output
 */
public class JsonAddressesToCsv {

	String name;
	String telephone;
	String streetAddress;
	String postalCode;
	String addressLocality;

	/**
	 * Start conversion
	 * 
	 * @param args Not used
	 * @throws IOException Thrown if input file cannot be read
	 */
	public static void main(String[] args) throws IOException {
		JsonAddressesToCsv jsonAddressesToCsv = new JsonAddressesToCsv();
		jsonAddressesToCsv.convert();
	}

	private void convert() throws IOException {
		String rawData = new String(
				Files.readAllBytes(Paths.get("test/conversion/input/input-file.json")));
		JsonNode dataAsJson = Json.parse(rawData);
		List<String> outputLines = new ArrayList<>();
		outputLines.add("Name;PLZ;Stadt;Straße;Telefon");

		for (JsonNode organisation : dataAsJson) {
			if (organisation.get("name") != null) {
				name = organisation.get("name").asText();

				if (organisation.get("telephone") != null)
					telephone = organisation.get("telephone").asText();
				else
					telephone = "";

				for (JsonNode location : organisation.withArray("location")) {
					JsonNode address = location.get("address");
					if (address != null) {
						if (address.get("streetAddress") != null)
							streetAddress = address.get("streetAddress").asText();
						else
							streetAddress = "";
						if (address.get("postalCode") != null)
							postalCode = address.get("postalCode").asText();
						else
							postalCode = "";
						if (address.get("addressLocality") != null)
							addressLocality = address.get("addressLocality").asText();
						else
							addressLocality = "";
					}
				}
				outputLines.add(name + ";" + postalCode + ";" + addressLocality + ";"
						+ streetAddress + ";" + telephone);
			}
		}
		Files.write(Paths.get("test/conversion/output/output-file.csv"),
				outputLines);
	}

}
