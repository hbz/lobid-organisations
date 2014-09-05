import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

/**
 * Tests with internal dependencies. Run locally.
 */
public class IntegrationTest {

    @Test
    public void testMainPage() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333/organisations");
                assertThat(browser.pageSource()).contains("lobid-organisations");
            }
        });
    }

    @Test
    public void getById() {
        running(fakeApplication(), () -> {
             Result result = route(fakeRequest(GET, "/organisations/ZY571"));
             assertThat(result).isNotNull();
             assertThat(contentType(result)).isEqualTo("application/json");
             assertThat(contentAsString(result)).contains("HBZ");
        });
    }

    @Test
    public void queryByField() {
        running(fakeApplication(), () -> {
             Result result = route(fakeRequest(GET, "/organisations/search?q=fundertype.value:land"));
             assertThat(result).isNotNull();
             assertThat(contentType(result)).isEqualTo("application/json");
             assertThat(contentAsString(result)).contains("Musikhochschule Lübeck");
        });
    }

}
