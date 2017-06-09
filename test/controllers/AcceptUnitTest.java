/* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 */

package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import play.api.http.MediaRange;
import play.api.mvc.Request;
import play.mvc.Http.RequestBody;
import play.test.FakeRequest;
import scala.collection.JavaConversions;

/**
 * Unit tests for functionality provided by the {@link Accept} class.
 * 
 * @author Fabian Steeg (fsteeg)
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class AcceptUnitTest {

	// test data parameters, formatted as "input /*->*/ expected output"
	@Parameters
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			// neither supported header nor supported format given, return default:
			{ fakeRequest(), null, /*->*/ "json" },
			{ fakeRequest(), "", /*->*/ "json" },
			{ fakeRequest(), "xml", /*->*/ "json" },
			{ fakeRequest().withHeader("Accept", ""), null, /*->*/ "json" },
			{ fakeRequest().withHeader("Accept", "application/pdf"), null, /*->*/ "json" },
			// no header, just format parameter:
			{ fakeRequest(), "html", /*->*/ "html" },
			{ fakeRequest(), "json", /*->*/ "json" },
			{ fakeRequest(), "json:isil", /*->*/ "json:isil" },
			{ fakeRequest(), "js", /*->*/ "js" },
			{ fakeRequest(), "csv", /*->*/ "csv" },
			{ fakeRequest(), "csv:name,id", /*->*/ "csv:name,id" },
			{ fakeRequest(), "rdf", /*->*/ "rdf" },
			{ fakeRequest(), "ttl", /*->*/ "ttl" },
			{ fakeRequest(), "nt", /*->*/ "nt" },
			// supported content types, no format parameter given:
			{ fakeRequest().withHeader("Accept", "text/html"), null, /*->*/ "html" },
			{ fakeRequest().withHeader("Accept", "application/javascript"), null, /*->*/ "js" },
			{ fakeRequest().withHeader("Accept", "text/javascript"), null, /*->*/ "js" },
			{ fakeRequest().withHeader("Accept", "application/json"), null, /*->*/ "json" },
			{ fakeRequest().withHeader("Accept", "application/ld+json"), null, /*->*/ "json" },
			{ fakeRequest().withHeader("Accept", "text/csv"), null, /*->*/ "csv" },
			{ fakeRequest().withHeader("Accept", "text/plain"), null, /*->*/ "nt" },
			{ fakeRequest().withHeader("Accept", "application/n-triples"), null, /*->*/ "nt" },
			{ fakeRequest().withHeader("Accept", "text/turtle"), null, /*->*/ "ttl" },
			{ fakeRequest().withHeader("Accept", "application/x-turtle"), null, /*->*/ "ttl" },
			{ fakeRequest().withHeader("Accept", "application/xml"), null, /*->*/ "rdf" },
			{ fakeRequest().withHeader("Accept", "application/rdf+xml"), null, /*->*/ "rdf" },
			{ fakeRequest().withHeader("Accept", "text/xml"), null, /*->*/ "rdf" },
			// we pick the preferred content type:
			{ fakeRequest().withHeader("Accept", "text/html,application/json"), null, /*->*/"html" },
			{ fakeRequest().withHeader("Accept", "application/json,text/html"), null, /*->*/ "json" },
			// format parameter overrides header:
			{ fakeRequest().withHeader("Accept", "text/html"), "json", /*->*/ "json" }});
	} // @formatter:on

	private FakeRequest fakeRequest;
	private String passedFormat;
	private String expectedFormat;

	public AcceptUnitTest(FakeRequest request, String givenFormat,
			String expectedFormat) {
		this.fakeRequest = request;
		this.passedFormat = givenFormat;
		this.expectedFormat = expectedFormat;
	}

	@Test
	public void test() {
		Request<RequestBody> request = fakeRequest.getWrappedRequest();
		Collection<MediaRange> acceptedTypes =
				JavaConversions.asJavaCollection(request.acceptedTypes());
		String description =
				String.format("resulting format for passedFormat=%s, acceptedTypes=%s",
						passedFormat, acceptedTypes);
		String result = Accept.formatFor(passedFormat, acceptedTypes);
		assertThat(result).as(description).isEqualTo(expectedFormat);
	}

}