/* Copyright 2016, hbz. Licensed under the Eclipse Public License 1.0 */

package controllers;

import java.util.Map;

import javax.annotation.Nullable;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.ScriptModule;

/**
 * Native script plugin to return 0.
 * 
 * Yes, this seems completely nuts. Let me explain: <br/>
 * 
 * We need a script that simply returns 0 as a workaround for location scoring,
 * see: <br/>
 * 
 * https://github.com/elastic/elasticsearch/issues/7788#issuecomment-57196342
 * https://github.com/elastic/elasticsearch/issues/18892#issuecomment-226544977
 * <br/>
 * 
 * With Groovy scripting this would be straight-forward, but we run in embedded
 * mode and use native scripting for security reasons, see: <br/>
 * 
 * https://github.com/hbz/lobid-organisations/commit/acefce9ba980f2f955c79da71e3ccc8068489d56
 * https://github.com/hbz/lobid-organisations/commit/1027537db9b26911d7294532d4f1ae44244205ce
 * <br/>
 * 
 * So we have a full native script for this very simple functionality.
 * 
 * @author Fabian Steeg (fsteeg)
 * 
 */
public class Zero extends Plugin {
	@Override
	public String name() {
		return "zero";
	}

	@Override
	public String description() {
		return "Native script to return 0";
	}

	/**
	 * @param scriptModule The scriptModule to register this script on
	 */
	public void onModule(ScriptModule scriptModule) {
		scriptModule.registerScript(name(), ZeroScriptFactory.class);
	}

	/** Factory to construct the script. */
	public static class ZeroScriptFactory implements NativeScriptFactory {
		@Override
		public ExecutableScript newScript(@Nullable Map<String, Object> params) {
			return new ZeroScript();
		}

		@Override
		public boolean needsScores() {
			return false;
		}
	}

	/** The actual script. */
	public static class ZeroScript extends AbstractSearchScript {

		@Override
		public Object run() {
			return 0;
		}
	}
}