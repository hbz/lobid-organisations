
/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import controllers.Index;
import controllers.Transformation;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
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

	private class ContentLanguageWrapper extends Action.Simple {
		public ContentLanguageWrapper(Action<?> action) {
			this.delegate = action;
		}

		@Override
		public Promise<Result> call(Http.Context context) throws Throwable {
			context.response().setHeader("Content-Language",
					controllers.Application.currentLang());
			return this.delegate.call(context);
		}
	}

	@Override
	public Action<?> onRequest(Http.Request request, Method actionMethod) {
		return new ContentLanguageWrapper(super.onRequest(request, actionMethod));
	}
}