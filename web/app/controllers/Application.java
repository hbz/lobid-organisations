package controllers;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

import play.*;
import play.api.mvc.AnyContent;
import play.api.mvc.Request;
import play.libs.Json;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render("lobid-organisations"));
	}

	public static Result context() {
		response().setContentType("application/ld+json");
		response().setHeader("Access-Control-Allow-Origin", "*");
		return ok(Play.application().resourceAsStream("context.jsonld"));
	}
}
