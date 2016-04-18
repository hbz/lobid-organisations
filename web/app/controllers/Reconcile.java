/* Copyright 2015 Fabian Steeg, hbz. Licensed under the Eclipse Public License 1.0 */

package controllers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
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

	public static Result meta(String callback) {
		ObjectNode result = Json.newObject();
		result.put("name", "lobid-organisations reconciliation");
		result.put("identifierSpace", "http://beta.lobid.org/organisations");
		result.put("schemaSpace", "http://beta.lobid.org/organisations");
		result.put("defaultTypes", TYPES);
		result.put("view", Json.newObject()//
				.put("url", "http://beta.lobid.org/organisations/{{id}}"));
		return callback.isEmpty() ? ok(result)
				: ok(String.format("/**/%s(%s);", callback, result.toString()))
						.as("application/json");
	}

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
			List<JsonNode> results = mapToResults(searchResponse.getHits());
			ObjectNode resultsForInputQuery = Json.newObject();
			resultsForInputQuery.put("result", Json.toJson(results));
			Logger.debug("r: " + resultsForInputQuery);
			response.put(inputQuery.getKey(), resultsForInputQuery);
		}
		return ok(response);
	}

	private static List<JsonNode> mapToResults(SearchHits searchHits) {
		return Arrays.asList(searchHits.getHits()).stream().map(hit -> {
			Map<String, Object> map = hit.getSource();
			ObjectNode resultForHit = Json.newObject();
			resultForHit.put("id", hit.getId());
			Object name = map.get("name");
			resultForHit.put("name", name == null ? "" : name + "");
			resultForHit.put("score", hit.getScore());
			resultForHit.put("type", TYPES);
			return resultForHit;
		}).collect(Collectors.toList());
	}

	private static SearchResponse executeQuery(Entry<String, JsonNode> entry,
			String queryString) {
		JsonNode limitNode = entry.getValue().get("limit");
		int limit = limitNode == null ? -1 : limitNode.asInt();
		SearchResponse response = Application.executeQuery(0, limit,
				QueryBuilders.queryStringQuery(queryString));
		return response;
	}

	private static String buildQueryString(Entry<String, JsonNode> entry) {
		String queryString = entry.getValue().get("query").asText();
		JsonNode props = entry.getValue().get("properties");
		if (props != null) {
			for (JsonNode p : props) {
				queryString += " " + p.get("v").asText();
			}
		}
		return queryString;
	}

}
