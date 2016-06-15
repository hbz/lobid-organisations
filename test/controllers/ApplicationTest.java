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

import java.util.Arrays;

import org.junit.Test;

import index.ElasticsearchTest;
import play.libs.Json;
import play.mvc.Result;

@SuppressWarnings("javadoc")
public class ApplicationTest extends ElasticsearchTest {

	@Test
	public void renderIndexTemplate() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET, "/organisations"));
			assertThat(contentType(result)).isEqualTo("text/html");
			assertThat(contentAsString(result)).contains("lobid-organisations");
		});
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

	@Test
	public void searchFormatDefault() {
		running(fakeApplication(), () -> {
			for (String f : Arrays.asList("", "&format=", "&format=json",
					"&format=whatever")) {
				play.Logger.debug("searchFormatDefault test for: " + f);
				Result result =
						route(fakeRequest(GET, "/organisations/search?q=*" + f));
				assertThat(result).isNotNull();
				assertThat(contentType(result)).isEqualTo("application/json");
				assertThat(header("Access-Control-Allow-Origin", result))
						.isEqualTo("*");
			}
		});
	}

	@Test
	public void searchFormatHtml() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/search?q=*&format=html"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("text/html");
		});
	}

	@Test
	public void searchFormatJs() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/search?q=*&format=js"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/javascript");
		});
	}

}