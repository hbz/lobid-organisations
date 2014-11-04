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

import play.mvc.Result;
import play.twirl.api.Content;

/**
 * Tests without external dependencies. Run in CI.
 */
@SuppressWarnings("javadoc")
public class ApplicationTest {

	@Test
	public void renderIndexTemplate() {
		Content html = views.html.index.render("Page title");
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
}