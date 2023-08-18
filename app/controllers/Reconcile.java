/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package controllers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * OpenRefine reconciliation service controller.
 * 
 * Serves reconciliation service meta data and multi query requests.
 * 
 * See https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation and
 * https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-API
 * 
 * @author Fabian Steeg (fsteeg)
 *
 */
public class Reconcile extends Controller {

	private static final JsonNode TYPES =
			Json.toJson(Arrays.asList("lobid-organisation"));

	/**
	 * @param callback The name of the JSONP function to wrap the response in
	 * @return OpenRefine reconciliation endpoint meta data, wrapped in `callback`
	 */
	public static Result meta(String callback) {
		ObjectNode result = Json.newObject();
		result.put("name", "lobid-organisations reconciliation");
		result.put("identifierSpace", "http://lobid.org/organisations");
		result.put("schemaSpace", "http://lobid.org/organisations");
		result.put("defaultTypes", TYPES);
		result.put("view", Json.newObject()//
				.put("url", "http://lobid.org/organisations/{{id}}"));
		return callback.isEmpty() ? ok(result)
				: ok(String.format("/**/%s(%s);", callback, result.toString()))
						.as("application/json");
	}

	/** @return Reconciliation data for the queries in the request */
	public static Result reconcile() {
		JsonNode request =
				Json.parse(request().body().asFormUrlEncoded().get("queries")[0]);
		Iterator<Entry<String, JsonNode>> inputQueries = request.fields();
		ObjectNode response = Json.newObject();
		while (inputQueries.hasNext()) {
			Entry<String, JsonNode> inputQuery = inputQueries.next();
			Logger.debug("q: " + inputQuery);
			SearchResponse searchResponse =
					executeQuery(inputQuery, buildQueryString(inputQuery));
			List<JsonNode> results =
					mapToResults(mainQuery(inputQuery), searchResponse.getHits());
			ObjectNode resultsForInputQuery = Json.newObject();
			resultsForInputQuery.put("result", Json.toJson(results));
			Logger.debug("r: " + resultsForInputQuery);
			response.put(inputQuery.getKey(), resultsForInputQuery);
		}
		return ok(response);
	}

	private static List<JsonNode> mapToResults(String mainQuery,
			SearchHits searchHits) {
		return Arrays.stream(searchHits.getHits()).map(hit -> {
			Map<String, Object> map = hit.getSource();
			ObjectNode resultForHit = Json.newObject();
			resultForHit.put("id", hit.getId());
			Object nameObject = map.get("name");
			String name = nameObject == null ? "" : nameObject + "";
			resultForHit.put("name", name);
			resultForHit.put("score", hit.getScore());
			resultForHit.put("match", mainQuery.equalsIgnoreCase(name));
			resultForHit.put("type", TYPES);
			return resultForHit;
		}).collect(Collectors.toList());
	}

	private static SearchResponse executeQuery(Entry<String, JsonNode> entry,
			String queryString) {
		JsonNode limitNode = entry.getValue().get("limit");
		int limit = limitNode == null ? -1 : limitNode.asInt();
		SimpleQueryStringBuilder stringQuery =
				QueryBuilders.simpleQueryStringQuery(queryString);
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(stringQuery)
				.must(QueryBuilders.existsQuery("type"));
		return Index.executeQuery(0, limit, boolQuery, "");
	}

	private static String buildQueryString(Entry<String, JsonNode> entry) {
		String queryString = mainQuery(entry);
		JsonNode props = entry.getValue().get("properties");
		if (props != null) {
			for (JsonNode p : props) {
				queryString += " " + p.get("v").asText();
			}
		}
		return queryString;
	}

	private static String mainQuery(Entry<String, JsonNode> entry) {
		return entry.getValue().get("query").asText();
	}

}
