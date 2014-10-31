import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
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
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.assertEquals;


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
