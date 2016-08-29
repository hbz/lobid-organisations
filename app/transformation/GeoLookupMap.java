package transformation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import controllers.Application;
import play.Logger;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.mvc.Http.Status;

/**
 * A map that looks up geo locations for adresses.
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class GeoLookupMap extends HashMap<String, String> {

	private static final long serialVersionUID = 1L;
	private static final String SERVER =
			Application.CONFIG.getString("transformation.geo.lookup.server");
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
		// TODO avoid '__default' calls
		// TODO avoid duplicate lat/long calls for same key
		// (will be possible after https://github.com/hbz/geodata/issues/27)
		// TODO pass key as query param, not in URI path
		// (will be possible after https://github.com/hbz/geodata/issues/26)
		if (!key.equals("__default")) {
			try {
				String path =
						"/geodata" + (lookupType == LookupType.LON ? "/long/" : "/lat/")
								+ key.toString();
				URI uri = new URI("http", null, SERVER.split(":")[0],
						Integer.parseInt(SERVER.split(":")[1]), path, null, null);
				String url = uri.toASCIIString();
				Promise<String> promise = WS.url(url).get().map(response -> {
					if (response.getStatus() == Status.OK) {
						return response.getBody();
					}
					Logger.error(
							"Geo lookup response status not OK. Key={}, URL={}, Status: {} ({})",
							key, url, response.getStatus(), response.getStatusText());
					return null;
				});
				return promise.get(1, TimeUnit.MINUTES);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
}
