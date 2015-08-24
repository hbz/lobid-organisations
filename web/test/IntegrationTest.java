import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Result;
import play.test.TestBrowser;

/**
 * Tests with internal dependencies. Run locally.
 */
@SuppressWarnings("javadoc")
public class IntegrationTest {

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
			Result result = route(fakeRequest(GET, "/organisations/DE-9"));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/json");
			assertThat(contentAsString(result)).contains("Greifswald");
		});
	}

	@Test
	public void queryByField() {
		running(
				fakeApplication(),
				() -> {
					Result result =
							route(fakeRequest(GET,
									"/organisations/search?q=fundertype.value:land&size=2000"));
					assertThat(result).isNotNull();
					assertThat(contentType(result)).isEqualTo("application/json");
					assertThat(contentAsString(result)).contains(
							"Bayerisches Landesvermessungsamt");
				});
	}

	@Test
	public void rectangleSearch() {
		running(
				fakeApplication(),
				() -> {
					Result result =
							route(fakeRequest(
									GET,
									"/organisations/search?q=fundertype.value:land&location=52,12+53,12+53,14+52,14"));
					assertThat(result).isNotNull();
					assertThat(contentType(result)).isEqualTo("application/json");
					assertThat(contentAsString(result)).contains("Berlin");
				});
	}

	@Test
	public void triangleSearch() {
		running(
				fakeApplication(),
				() -> {
					Result result =
							route(fakeRequest(GET,
									"/organisations/search?q=fundertype.value:land&location=54,15+56,14+56,12"));
					assertThat(result).isNotNull();
					assertThat(contentType(result)).isEqualTo("application/json");
				});
	}

	@Test
	public void hexagonSearch() {
		running(
				fakeApplication(),
				() -> {
					Result result =
							route(fakeRequest(
									GET,
									"/organisations/search?q=fundertype.value:land&location=54,15+56,14+56,12+54,10+52,11+53,14"));
					assertThat(result).isNotNull();
					assertThat(contentType(result)).isEqualTo("application/json");
				});
	}

	@Test
	public void distanceSearch() {
		running(
				fakeApplication(),
				() -> {
					Result result =
							route(fakeRequest(GET,
									"/organisations/search?q=fundertype.value:land&location=52.52,13.39,25"));
					assertThat(result).isNotNull();
					assertThat(contentType(result)).isEqualTo("application/json");
					assertThat(contentAsString(result)).contains("Berlin");
				});
	}

	@Test
	public void reconcileRequest() {
		running(fakeApplication(), () -> {
			Result result = route(
					fakeRequest(POST, "/organisations/reconcile").withFormUrlEncodedBody(
							ImmutableMap.of("queries", "{\"q99\":{\"query\":\"*\"}}")));
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo("application/json");
			assertThat(Json.parse(contentAsString(result))).isNotNull();
			assertThat(contentAsString(result)).contains("q99");
		});
	}
}