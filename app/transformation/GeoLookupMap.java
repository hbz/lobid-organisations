package transformation;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Application;
import play.Logger;
import play.cache.Cache;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.mvc.Http.Status;

/**
 * A map that looks up geo locations for adresses.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class GeoLookupMap extends HashMap<String, String> {

	private static final long serialVersionUID = 1L;
	private static final String API =
			Application.CONFIG.getString("transformation.geo.lookup.server");
	private static final String API_KEY =
			Application.CONFIG.getString("transformation.geo.lookup.key");
	private static final Double THRESHOLD =
			Application.CONFIG.getDouble("transformation.geo.lookup.threshold");
	private LookupType lookupType;

	static enum LookupType {
		LAT, LON
	}

	/**
	 * @param lookupType The lookup type, see {@link LookupType}
	 */
	GeoLookupMap(LookupType lookupType) {
		this.lookupType = lookupType;
	}

	@Override
	public String get(Object key) {
		JsonNode cached = (JsonNode) Cache.get(key.toString());
		if (cached != null) {
			return resultFromNode(cached);
		}
		if (!key.equals("__default")) {
			String[] fullAddress = key.toString().split("_");
			String street = clean(fullAddress[0]);
			String postcode = fullAddress[1];
			String city = clean(fullAddress[2]);
			String country = fullAddress[3];
			String query = // e.g. "Jülicher Str 6, 50674 Köln"
					String.format("%s, %s %s", street, postcode, city);
			WSRequestHolder requestHolder = WS.url(API)//
					.setQueryParameter("api_key", API_KEY)
					.setQueryParameter("layers", "address")
					.setQueryParameter("boundary.country", country.trim())
					.setQueryParameter("text", query);
			Logger.debug("Calling API={} with params={}", requestHolder.getUrl(),
					requestHolder.getQueryParameters());
			String result = callApi(key, street, city, postcode, requestHolder);
			delay();
			return result;
		}
		return null;
	}

	private String callApi(Object key, String street, String city,
			String postalcode, WSRequestHolder requestHolder) {
		Promise<String> promise = requestHolder.get().map(response -> {
			String details = String.format(
					"no result returned for API call with URL=%s, params=%s",
					requestHolder.getUrl(), requestHolder.getQueryParameters());
			if (response.getStatus() == Status.OK) {
				JsonNode features = response.asJson().findValue("features");
				if (features.size() > 0) {
					JsonNode bestResult = features.get(0);
					JsonNode coordinates = bestResult.findValue("coordinates");
					JsonNode confidence = bestResult.findValue("confidence");
					JsonNode bestStreet = bestResult.findValue("street");
					JsonNode bestCity = bestResult.findValue("locality");
					JsonNode bestPostalcode = bestResult.findValue("postalcode");
					if (coordinates != null && isGoodEnough(street, city, postalcode,
							bestStreet, bestCity, bestPostalcode, confidence)) {
						Cache.set(key.toString(), coordinates);
						return resultFromNode(coordinates);
					}
					// we have a result, but it's not good enough, provide some details:
					details = String.format(
							"best result with confidence=%s, "
									+ "street=%s, housenumber=%s, postalcode=%s, locality=%s, coordinates=%s",
							confidence, bestStreet, bestResult.findValue("housenumber"),
							bestPostalcode, bestCity, coordinates);
				}
				// response OK, but no result, remember that to avoid redundant calls
				Cache.set(key.toString(), Json.newObject());
			}
			Logger.error(
					"No geo coordinates found for input data: {}, API query: {}, status: {} ({}), details: {}",
					key, requestHolder.getQueryParameters().get("text"),
					response.getStatus(), response.getStatusText(), details);
			return null;
		});
		return promise.get(1, TimeUnit.MINUTES);

	}

	private static boolean isGoodEnough(String street, String city,
			String postalcode, JsonNode bestStreet, JsonNode bestCity,
			JsonNode bestPostalcode, JsonNode confidence) {
		boolean streetMatch =
				bestStreet != null && street.contains(bestStreet.textValue());
		boolean cityMatch = bestCity != null && city.equals(bestCity.textValue());
		boolean postalMatch =
				bestPostalcode != null && postalcode.equals(bestPostalcode.textValue());
		boolean addressMatch = streetMatch && (cityMatch || postalMatch);
		boolean overTreshold = confidence != null && confidence.isDouble()
				&& confidence.asDouble() >= THRESHOLD;
		return overTreshold || addressMatch;
	}

	private static String clean(String query) {
		return query.replaceAll("tr\\.", "traße").replaceAll("[.,]", " ")
				.replaceAll("\\s+", " ").trim();
	}

	private static void delay() {
		// 6 requests per second. see
		// https://mapzen.com/documentation/search/api-keys-rate-limits/
		try {
			Thread.sleep(1000 / 6);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String resultFromNode(JsonNode node) {
		if (node.size() == 0) {
			return null;
		}
		String lon = node.get(0).toString();
		String lat = node.get(1).toString();
		return this.lookupType == LookupType.LAT ? lat : lon;
	}
}
