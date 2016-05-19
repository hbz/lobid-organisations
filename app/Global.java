import java.io.File;
import java.io.IOException;

import controllers.Index;
import controllers.Transformation;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import transformation.Enrich;

/**
 * Application global settings.
 * 
 * See https://www.playframework.com/documentation/2.3.x/JavaGlobal
 * 
 * @author Simon Ritter (SBRitter), Fabian Steeg (fsteeg)
 */
public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		super.onStart(app);
		if (!app.isTest()) {
			initData();
		}
	}

	@Override
	public void onStop(Application app) {
		if (!app.isTest()) {
			Index.close();
		}
		super.onStop(app);
	}

	private static void initData() {
		try {
			if (new File(Enrich.DATA_OUTPUT_FILE).exists()) {
				Logger.info(
						"Transformation output file exists, indexing only. "
								+ "To trigger transformation on start, delete '{}'",
						Enrich.DATA_OUTPUT_FILE);
			} else {
				Logger.info("Starting transformation, will write to '{}' ",
						Enrich.DATA_OUTPUT_FILE);
				Transformation.transformSet();
			}
			Index.initialize(Enrich.DATA_OUTPUT_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}