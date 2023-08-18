/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import index.ElasticsearchTest;
import play.mvc.Result;
import play.test.FakeRequest;

/**
 * Integration tests for functionality provided by the {@link Accept} class.
 * 
 * @author Fabian Steeg (fsteeg)
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class AcceptIntegrationTest extends ElasticsearchTest {

	// test data parameters, formatted as "input /*->*/ expected output"
	@Parameters
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			// search, default format: JSON
			{ fakeRequest(GET, "/organisations/search?q=*"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/search?q=*&format="), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/search?q=*&format=json"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/search?q=*&format=whatever"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/search?q=*").withHeader("Accept", "text/plain"), /*->*/ "application/json" },
			// search, others formats as query param:
			{ fakeRequest(GET, "/organisations/search?q=*&format=html"), /*->*/ "text/html" },
			{ fakeRequest(GET, "/organisations/search?q=*&format=js"), /*->*/ "application/javascript" },
			{ fakeRequest(GET, "/organisations/search?q=*&format=csv"), /*->*/ "text/csv" },
			// search, others formats via header:
			{ fakeRequest(GET, "/organisations/search?q=*").withHeader("Accept", "application/json"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/search?q=*").withHeader("Accept", "text/html"), /*->*/ "text/html" },
			{ fakeRequest(GET, "/organisations/search?q=*").withHeader("Accept", "application/javascript"), /*->*/ "application/javascript" },
			{ fakeRequest(GET, "/organisations/search?q=*").withHeader("Accept", "text/csv"), /*->*/ "text/csv" },
			// get, default format: JSON
			{ fakeRequest(GET, "/organisations/DE-38"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/DE-38?format="), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/DE-38?format=json"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/DE-38?format=whatever"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "text/plain"), /*->*/ "application/n-triples" },
			// get, other formats as query param:
			{ fakeRequest(GET, "/organisations/DE-38?format=html"), /*->*/ "text/html" },
			{ fakeRequest(GET, "/organisations/DE-38?format=js"), /*->*/ "application/javascript" },
			{ fakeRequest(GET, "/organisations/DE-38?format=rdf"), /*->*/ "application/rdf+xml" },
			{ fakeRequest(GET, "/organisations/DE-38?format=ttl"), /*->*/ "text/turtle" },
			{ fakeRequest(GET, "/organisations/DE-38?format=nt"), /*->*/ "application/n-triples" },
			// get, other formats as path elem:
			{ fakeRequest(GET, "/organisations/DE-38.html"), /*->*/ "text/html" },
			{ fakeRequest(GET, "/organisations/DE-38.js"), /*->*/ "application/javascript" },
			{ fakeRequest(GET, "/organisations/DE-38.rdf"), /*->*/ "application/rdf+xml" },
			{ fakeRequest(GET, "/organisations/DE-38.ttl"), /*->*/ "text/turtle" },
			{ fakeRequest(GET, "/organisations/DE-38.nt"), /*->*/ "application/n-triples" },
			// get, others formats via header:
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/json"), /*->*/ "application/json" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "text/html"), /*->*/ "text/html" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/javascript"), /*->*/ "application/javascript" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "text/xml"), /*->*/ "application/rdf+xml" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/xml"), /*->*/ "application/rdf+xml" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/rdf+xml"), /*->*/ "application/rdf+xml" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "text/turtle"), /*->*/ "text/turtle" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/x-turtle"), /*->*/ "text/turtle" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "text/plain"), /*->*/ "application/n-triples" },
			{ fakeRequest(GET, "/organisations/DE-38").withHeader("Accept", "application/n-triples"), /*->*/ "application/n-triples" }});
	} // @formatter:on

	private final FakeRequest fakeRequest;
	private final String contentType;

	public AcceptIntegrationTest(FakeRequest request, String contentType) {
		this.fakeRequest = request;
		this.contentType = contentType;
	}

	@Test
	public void test() {
		running(fakeApplication(), () -> {
			Result result = route(fakeRequest);
			assertThat(result).isNotNull();
			assertThat(contentType(result)).isEqualTo(contentType);
			if (contentType.equals("application/json")) {
				assertThat(header("Access-Control-Allow-Origin", result))
						.isEqualTo("*");
			}
		});
	}

}