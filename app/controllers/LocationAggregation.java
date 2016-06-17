package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.ScriptModule;

/**
 * Native script plugin to build custom location aggregation.
 * 
 * @author Fabian Steeg (fsteeg)
 * 
 */
public class LocationAggregation extends Plugin {
	@Override
	public String name() {
		return "location-aggregation";
	}

	@Override
	public String description() {
		return "Native script to build custom location aggregation.";
	}

	/**
	 * @param scriptModule The scriptModule to register this script on
	 */
	public void onModule(ScriptModule scriptModule) {
		scriptModule.registerScript(name(), LocationScriptFactory.class);
	}

	/** Factory to construct the LocationScript. */
	public static class LocationScriptFactory implements NativeScriptFactory {
		@Override
		public ExecutableScript newScript(@Nullable Map<String, Object> params) {
			return new LocationScript();
		}

		@Override
		public boolean needsScores() {
			return false;
		}
	}

	/** The actual script to create the custom location aggregation. */
	public static class LocationScript extends AbstractSearchScript {

		@Override
		public Object run() {
			try {
				HashMap<?, ?> location =
						(HashMap<?, ?>) ((ArrayList<?>) source().get("location")).get(0);
				HashMap<?, ?> geo = (HashMap<?, ?>) location.get("geo");
				String latLon = String.format("%s,%s", geo.get("lat"), geo.get("lon"));
				Object classificationId =
						((HashMap<?, ?>) source().get("classification")).get("id");
				Object id = ((String) source().get("id"))
						.replaceAll("http://([^.]+.)?lobid.org/organisations/", "")
						.replace("#!", "");
				return String.format("%s;;;%s;;;%s;;;%s", id, latLon,
						source().get("name"), classificationId);
			} catch (NullPointerException e) {
				// We need all four fields, if any is missing, we can set all to null
				return "null;;;null;;;null;;;null";
			}
		}
	}
}