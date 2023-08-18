/* Copyright 2017 Fabian Steeg, hbz. Licensed under the EPL 2.0 */

package index;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.GET;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import scala.Option;

/**
 * Test customizable aggregations.
 * 
 * See https://github.com/hbz/lobid-organisations/pull/307
 * 
 * @author Fabian Steeg (fsteeg)
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class AggregationsTest extends ElasticsearchTest {

	// test data parameters, formatted as "input /*->*/ expected output"
	@Parameters(name = "{0} -> {1}")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			// neither supported header nor supported format given, return default:
			{ "", /*->*/ 0, Status.OK },
			{ "&aggregations=", /*->*/ 0, Status.OK },
			{ "&aggregations=type", /*->*/ 1, Status.OK },
			{ "&aggregations=type,classification.label.de", /*->*/ 2, Status.OK },
			{ "&aggregations=invalid", /*->*/ 0, Status.BAD_REQUEST },});
	} // @formatter:on

	private final FakeRequest fakeRequest;
	private final int expectedNumberOfAggragations;
	private final int expectedResponseStatus;

	public AggregationsTest(String param, int expectedNumberOfAggragations,
			int status) {
		this.fakeRequest =
				fakeRequest(GET, "/organisations/search?format=json&q=*" + param);
		this.expectedNumberOfAggragations = expectedNumberOfAggragations;
		this.expectedResponseStatus = status;
	}

	@Test
	public void test() {
		running(fakeApplication(), () -> {
			Result route = route(fakeRequest);
			int responseStatus = Helpers.status(route);
			assertThat(responseStatus).as("response status")
					.isEqualTo(expectedResponseStatus);
			if (responseStatus == Status.OK) {
				JsonNode aggregationNode =
						Json.parse(Helpers.contentAsString(route)).findValue("aggregation");
				Option<String> aggregationsQueryParam =
						fakeRequest.getWrappedRequest().getQueryString("aggregations");
				if (!aggregationsQueryParam.isDefined()
						|| aggregationsQueryParam.get().equals("")) {
					assertThat(aggregationNode).isNull();
				} else {
					int numberOfAggregations =
							Json.fromJson(aggregationNode, Map.class).keySet().size();
					assertThat(numberOfAggregations)
							.as("resulting number of aggregations")
							.isEqualTo(expectedNumberOfAggragations);
				}
			}
		});

	}

}