package controllers;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

import play.*;
import play.api.mvc.AnyContent;
import play.api.mvc.Request;
import play.cache.Cached;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {

	private static final String ES_SERVER = "http://weywot2.hbz-nrw.de:9200";
	private static final String ES_INDEX = "organisations";
	private static final String ES_TYPE = "dbs";

	public static Result index() {
		return ok(index.render("lobid-organisations"));
	}

	public static Result context() {
		response().setContentType("application/ld+json");
		response().setHeader("Access-Control-Allow-Origin", "*");
		return ok(Play.application().resourceAsStream("context.jsonld"));
	}

	public static Promise<Result> search(String q) {
		String url = String.format("%s/%s/_search", ES_SERVER, ES_INDEX);
		return WS.url(url).setQueryParameter("q", q).execute().map(x -> ok(x.asJson()));
	}

	public static Promise<Result> get(String id) {
		String url = String.format("%s/%s/%s/%s/_source", ES_SERVER, ES_INDEX, ES_TYPE, id);
		return WS.url(url).execute().map(x -> ok(x.asJson()));
	}
}
