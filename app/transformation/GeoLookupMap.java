package transformation;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Application;
import play.Logger;
import play.cache.Cache;
import play.libs.F.Promise;
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
			String city = fullAddress[1];
			String country = fullAddress[2];
			WSRequestHolder requestHolder = WS.url(API)//
					.setQueryParameter("api_key", API_KEY)
					.setQueryParameter("layers", "address")
					.setQueryParameter("boundary.country", country.trim())
					.setQueryParameter("text", clean(street) + ", " + clean(city));
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
			if (response.getStatus() == Status.OK) {
				List<JsonNode> coordinates =
						response.asJson().findValues("coordinates");
				if (coordinates != null && coordinates.size() > 0) {
					JsonNode node = coordinates.get(0);
					Cache.set(key.toString(), node);
					return resultFromNode(node);
				}
			}
			Logger.error("Geo lookup failed. Key={}, Params={}, Status: {} ({})", key,
					requestHolder.getQueryParameters(), response.getStatus(),
					response.getStatusText());
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
		String lon = node.get(0).toString();
		String lat = node.get(1).toString();
		return this.lookupType == LookupType.LAT ? lat : lon;
	}
}
