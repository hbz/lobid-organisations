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
			String street = fullAddress[0];
			String postcode = fullAddress[1];
			String city = fullAddress[2];
			String country = fullAddress[3];
			String query = // e.g. "Jülicher Str 6, 50674 Köln"
					String.format("%s, %s %s", clean(street), postcode, clean(city));
			WSRequestHolder requestHolder = WS.url(API)//
					.setQueryParameter("api_key", API_KEY)
					.setQueryParameter("layers", "address")
					.setQueryParameter("boundary.country", country.trim())
					.setQueryParameter("text", query);
			Logger.debug("Calling API={} with params={}", requestHolder.getUrl(),
					requestHolder.getQueryParameters());
			String result = callApi(key, requestHolder);
			delay();
			return result;
		}
		return null;
	}

	private String callApi(Object key, WSRequestHolder requestHolder) {
		Promise<String> promise = requestHolder.get().map(response -> {
			String details = String.format(
					"no result returned for API call with URL=%s, params=%s",
					requestHolder.getUrl(), requestHolder.getQueryParameters());
			if (response.getStatus() == Status.OK) {
				JsonNode json = response.asJson();
				JsonNode coordinates = json.findValue("coordinates");
				JsonNode confidence = json.findValue("confidence");
				if (coordinates != null && confidence != null//
						&& confidence.isDouble() && confidence.asDouble() >= THRESHOLD) {
					Cache.set(key.toString(), coordinates);
					return resultFromNode(coordinates);
				}
				// response OK, but no result, remember that to avoid redundant calls
				Cache.set(key.toString(), Json.newObject());
				details = String.format(
						"best result with confidence=%s, "
								+ "street=%s, housenumber=%s, postalcode=%s, locality=%s, coordinates=%s",
						confidence, json.findValue("street"), json.findValue("housenumber"),
						json.findValue("postalcode"), json.findValue("locality"),
						coordinates);
			}
			Logger.error(
					"No geo coordinates found for query: {}, status: {} ({}), details: {}",
					requestHolder.getQueryParameters().get("text"), response.getStatus(),
					response.getStatusText(), details);
			return null;
		});
		return promise.get(1, TimeUnit.MINUTES);

	}

	private static String clean(String query) {
		return query.replaceAll("[.,]", " ").replaceAll("\\s+", " ").trim();
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
