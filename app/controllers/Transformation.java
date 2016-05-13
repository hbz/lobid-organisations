package controllers;

import java.io.IOException;

import play.mvc.Controller;
import play.mvc.Result;
import transformation.Enrich;

/**
 * Controller to start the transformation of the data
 * 
 * @author Simon Ritter, Fabian Steeg
 *
 */
public class Transformation extends Controller {

	/**
	 * @return 200 ok or 403 forbidden response depending on ip address of client
	 * @throws IOException if data files cannot be read
	 */
	public static Result startTransformation() throws IOException {
		String remote = request().remoteAddress();
		if (!Application.CONFIG.getStringList("index.remote").contains(remote)) {
			return forbidden();
		}
		transformSet();
		return ok("Started transformation");
	}

	/**
	 * @return 200 ok if transformation can be started, otherwise 500
	 *         internalServerError if transformation fails, or 403 forbidden
	 *         depending on ip address of client
	 * @throws IOException If data files cannot be read
	 */
	public static Result transformSet() throws IOException {
		try {
			String startOfUpdates =
					Application.CONFIG.getString("transformation.updates.start");
			String intervalSize =
					Application.CONFIG.getString("transformation.updates.interval.size");
			String geoLookupServer =
					Application.CONFIG.getString("transformation.geo.lookup.server");
			String outputPath = Enrich.DATA_OUTPUT_FILE;
			Enrich.process(startOfUpdates, Integer.parseInt(intervalSize), outputPath,
					geoLookupServer);
		} catch (Exception e) {
			e.printStackTrace();
			return internalServerError("Transformation failed");
		}
		return ok("Transforming full data");
	}

}
