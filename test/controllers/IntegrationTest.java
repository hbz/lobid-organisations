/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import index.ElasticsearchTest;
import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Result;
import play.test.TestBrowser;

@SuppressWarnings("javadoc")
public class IntegrationTest extends ElasticsearchTest {

	@Test
	public void testMainPage() {
		running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT,
				new Callback<TestBrowser>() {
					@Override
					public void invoke(TestBrowser browser) {
						browser.goTo("http://localhost:3333/organisations");
						assertThat(browser.pageSource()).contains("lobid-organisations");
					}
				});
	}

	@Test
	public void getById() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET, "/organisations/DE-38"));
			assertContains(result, "Köln");
		});
	}

	@Test
	public void queryByField() {
		running(fakeApplication(), () -> {
			Result result = route(
					fakeRequest(GET, "/organisations/search?q=fundertype.label.de:land"));
			assertContains(result, "Köln");
		});
	}

	@Test
	public void queryByFieldIdWildcard() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/search?q=id:*DE-3*"));
			assertContains(result, "Köln");
		});
	}

	@Test
	public void queryByFieldIsilWildcard() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/search?q=isil:DE-3*"));
			assertContains(result, "Köln");
		});
	}

	@Test
	public void prettyGetById() {
		running(fakeApplication(), () -> {
			assertPretty(route(fakeRequest(GET, "/organisations/DE-38")));
		});
	}

	@Test
	public void prettyQueryByField() {
		running(fakeApplication(), () -> {
			assertPretty(route(
					fakeRequest(GET, "/organisations/search?q=fundertype.value:land")));
		});
	}

	@Test
	public void rectangleSearch() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET,
					"/organisations/search?q=fundertype.label.de:Körperschaft&location=52,13+52,14+53,14+53,13"));
			assertContains(result, "Berlin");
		});
	}

	@Test
	public void triangleSearch() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET,
					"/organisations/search?q=fundertype.label.de:Körperschaft&location=52,13.5+53,13+53,14"));
			assertContains(result, "Berlin");
		});
	}

	@Test
	public void hexagonSearch() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET,
					"/organisations/search?q=fundertype.label.de:Körperschaft&location=52,13+52,14+53,14+53,13+52.5,12+52.5,15"));
			assertContains(result, "Berlin");
		});
	}

	@Test
	public void distanceSearch() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET,
					"/organisations/search?q=fundertype.label.de:Körperschaft&location=52.5,13.3,25"));
			assertContains(result, "Berlin");
		});
	}

	@Test
	public void reconcileRequest() {
		running(fakeApplication(), () -> {
			Result result = route(
					fakeRequest(POST, "/organisations/reconcile").withFormUrlEncodedBody(
							ImmutableMap.of("queries", "{\"q99\":{\"query\":\"*\"}}")));
			assertContains(result, "q99");
		});
	}

	@Test
	public void suggestionsWithoutCallback() {
		running(fakeApplication(), () -> {
			Result result =
					route(fakeRequest(GET, "/organisations/search?q=*&format=json:name"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/json");
			assertThat(Json.parse(contentAsString(result))).isNotNull();
			assertThat(contentAsString(result)).contains("label").contains("id")
					.contains("category");
		});
	}

	@Test
	public void suggestionsWithCallback() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest(GET,
					"/organisations/search?q=*&format=json:name&callback=test"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/javascript");
			assertThat(contentAsString(result)).contains("test(").contains("label")
					.contains("id").contains("category");
		});
	}

	private static void assertPretty(Result result) {
		String contentAsString = contentAsString(result);
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jsonNode = mapper.readTree(contentAsString);
			String prettyString =
					mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
			assertThat(contentAsString).isEqualTo(prettyString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void assertContains(Result result, String content) {
		assertThat(result).isNotNull();
		assertThat(contentType(result)).isEqualTo("application/json");
		assertThat(Json.parse(contentAsString(result))).isNotNull();
		assertThat(contentAsString(result)).contains(content);
	}
}