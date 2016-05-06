package controllers;

import java.io.File;
import java.io.IOException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import play.mvc.Controller;
import play.mvc.Result;
import transformation.Enrich;
import transformation.EnrichSample;

/**
 * Controller to start the transformation of the data
 * 
 * @author Simon Ritter, Fabian Steeg
 *
 */
public class Transformation extends Controller {

	private static final Config CONFIG =
			ConfigFactory.parseFile(new File("conf/application.conf")).resolve();

	/**
	 * @return 200 ok or 403 forbidden response depending on ip address of client
	 * @throws IOException if data files cannot be read
	 */
	public static Result startFullTransformation() throws IOException {
		String remote = request().remoteAddress();
		if (!CONFIG.getStringList("index.remote").contains(remote)) {
			return forbidden();
		}
		transformFullSet();
		return ok("Started transformation");
	}

	/**
	 * @return 200 ok if transformation can be started, otherwise 500
	 *         internalServerError if transformation fails, or 403 forbidden
	 *         depending on ip address of client
	 * @throws IOException If data files cannot be read
	 */
	public static Result transformFullSet() throws IOException {
		try {
			String startOfUpdates = CONFIG.getString("transformation.updates.start");
			String intervalSize =
					CONFIG.getString("transformation.updates.interval.size");
			String geoLookupServer =
					CONFIG.getString("transformation.geo.lookup.server");
			String outputPath = CONFIG.getString("index.file.path");
			Enrich.process(startOfUpdates, Integer.parseInt(intervalSize), outputPath,
					geoLookupServer);
		} catch (Exception e) {
			e.printStackTrace();
			return internalServerError("Transformation failed");
		}
		return ok("Transforming full data");
	}

	/**
	 * @return 200 ok
	 * @throws IOException If data files cannot be read
	 */
	public static Result startSampleTransformation() throws IOException {
		EnrichSample.processSample(CONFIG.getString("index.file.path"));
		return ok("Transforming data");
	}

}
