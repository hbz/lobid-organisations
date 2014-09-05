import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.RequiredValidator;
import play.i18n.Lang;
import play.libs.F;
import play.libs.F.*;
import play.twirl.api.Content;
import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;


/**
 * Tests without external dependencies. Run in CI.
 */
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
