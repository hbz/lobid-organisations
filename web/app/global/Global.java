package global;

import java.io.IOException;

import controllers.Index;
import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		Logger.info("Application has started. Starting indexing now.");
		try {
			Index.initializeIndex();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}