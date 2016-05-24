package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import org.junit.Test;

import index.ElasticsearchTest;
import play.libs.Json;
import play.mvc.Result;
import play.twirl.api.Content;

@SuppressWarnings("javadoc")
public class ApplicationTest extends ElasticsearchTest {

	@Test
	public void renderIndexTemplate() {
		Content html = views.html.search.render("Page title", "", "[]");
		assertThat(contentType(html)).isEqualTo("text/html");
		assertThat(contentAsString(html)).contains("Page title");
	}

	@Test
	public void contextContentTypeAndCorsHeader() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET, "/organisations/context.jsonld"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/ld+json");
			assertThat(header("Access-Control-Allow-Origin", result)).isEqualTo("*");
		});
	}

	@Test
	public void reconcileMetadataRequestNoCallback() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET, "/organisations/reconcile"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/json");
			assertThat(Json.parse(contentAsString(result))).isNotNull();
		});
	}

	@Test
	public void reconcileMetadataRequestWithCallback() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/reconcile?callback=jsonp"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/json");
			assertThat(contentAsString(result)).startsWith("/**/jsonp(");
		});
	}
}