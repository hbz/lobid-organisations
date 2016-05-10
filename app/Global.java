import java.io.IOException;

import controllers.Index;
import controllers.Transformation;
import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * Application global settings.
 * 
 * See https://www.playframework.com/documentation/2.3.x/JavaGlobal
 * 
 * @author Simon Ritter (SBRitter)
 */
public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		Logger.info(
				"Application has started. Starting transformation and indexing now.");
		try {
			Transformation.transformSet();
			Index.initializeIndex(
					controllers.Application.CONFIG.getString("index.file.path"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}