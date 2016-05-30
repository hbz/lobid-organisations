package controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import play.Play;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.api;
import views.html.search;
import views.html.organisation;

/**
 * 
 * Contains the methods of the play application to search, get source, and
 * display context
 *
 */
public class Application extends Controller {

	/** The application config. */
	public static final Config CONFIG = ConfigFactory.load();

	private static final String ES_TYPE = CONFIG.getString("index.es.type");
	private static final String ES_NAME = CONFIG.getString("index.es.name");

	/**
	 * @return 200 ok response to render api documentation
	 */
	public static Result api() {
		return ok(api.render("lobid-organisations"));
	}

	/**
	 * @return JSON-LD context
	 */
	public static Result context() {
		response().setContentType("application/ld+json");
		response().setHeader("Access-Control-Allow-Origin", "*");
		return ok(Play.application().resourceAsStream("context.jsonld"));
	}

	/**
	 * @param q The search string
	 * @param location The geographical location to search in (polygon points or
	 *          single point plus distance)
	 * @param from From parameter for Elasticsearch query
	 * @param size Size parameter for Elasitcsearch query
	 * @param format The response format ('html' for HTML, else JSON)
	 * @return Result of search as ok() or badRequest()
	 * @throws JsonProcessingException Is thrown if response of search cannot be
	 *           processed as JsonNode
	 * @throws IOException Is thrown if response of search cannot be processed as
	 *           JsonNode
	 */
	public static Result search(String q, String location, int from, int size,
			String format) throws JsonProcessingException, IOException {
		try {
			if (q == null) {
				return ok(search.render("lobid-organisations", "", "[]", from, size))
						.as("text/html; charset=utf-8");
			}
			String result = null;
			if (location == null) {
				result = buildSimpleQuery(q, from, size);
			} else {
				result = prepareLocationQuery(location, q, from, size);
			}
			if (format != null && format.equals("html")) {
				play.Logger.debug("Result: {}", result);
				return ok(search.render("lobid-organisations", q, result, from, size))
						.as("text/html; charset=utf-8");
			}
			response().setHeader("Access-Control-Allow-Origin", "*");
			return ok(result).as("application/json; charset=utf-8");
		} catch (IllegalArgumentException x) {
			x.printStackTrace();
			return badRequest("Bad request: " + x.getMessage());
		}
	}

	private static String prepareLocationQuery(String location, String q,
			int from, int size) throws JsonProcessingException, IOException {
		String[] coordPairsAsString = location.split(" ");
		String result;
		if (coordPairsAsString[0].split(",").length > 2) {
			result = prepareDistanceQuery(coordPairsAsString, q, from, size);
		} else {
			result = preparePolygonQuery(coordPairsAsString, q, from, size);
		}
		return result;
	}

	private static String preparePolygonQuery(String[] coordPairsAsString,
			String q, int from, int size)
			throws JsonProcessingException, IOException {
		double[] latCoordinates = new double[coordPairsAsString.length];
		double[] lonCoordinates = new double[coordPairsAsString.length];
		String result;
		for (int i = 0; i < coordPairsAsString.length; i++) {
			String[] coordinatePair = coordPairsAsString[i].split(",");
			latCoordinates[i] = Double.parseDouble(coordinatePair[0]);
			lonCoordinates[i] = Double.parseDouble(coordinatePair[1]);
		}
		if (coordPairsAsString.length < 3) {
			throw new IllegalArgumentException(
					"Not enough points. Polygon requires more than two points.");
		}
		result = buildPolygonQuery(q, latCoordinates, lonCoordinates, from, size);
		return result;
	}

	private static String prepareDistanceQuery(String[] coordPairsAsString,
			String q, int from, int size)
			throws JsonProcessingException, IOException {
		String[] coordinatePair = coordPairsAsString[0].split(",");
		double lat = Double.parseDouble(coordinatePair[0]);
		double lon = Double.parseDouble(coordinatePair[1]);
		double distance = Double.parseDouble(coordinatePair[2]);
		String result;
		if (distance < 0) {
			throw new IllegalArgumentException(
					"Distance must not be smaller than 0.");
		}
		result = buildDistanceQuery(q, from, size, lat, lon, distance);
		return result;
	}

	private static String buildSimpleQuery(String q, int from, int size)
			throws JsonProcessingException, IOException {
		QueryBuilder simpleQuery = QueryBuilders.queryStringQuery(q);
		SearchResponse queryResponse = executeQuery(from, size, simpleQuery);
		return returnAsJson(queryResponse);
	}

	private static String buildPolygonQuery(String q, double[] latCoordinates,
			double[] lonCoordinates, int from, int size)
			throws JsonProcessingException, IOException {
		GeoPolygonQueryBuilder polygonQuery =
				QueryBuilders.geoPolygonQuery("location.geo");
		for (int i = 0; i < latCoordinates.length; i++) {
			polygonQuery.addPoint(latCoordinates[i], lonCoordinates[i]);
		}
		QueryBuilder simpleQuery = QueryBuilders.queryStringQuery(q);
		QueryBuilder polygonAndSimpleQuery =
				QueryBuilders.boolQuery().must(polygonQuery).must(simpleQuery);
		SearchResponse queryResponse =
				executeQuery(from, size, polygonAndSimpleQuery);
		return returnAsJson(queryResponse);
	}

	private static String buildDistanceQuery(String q, int from, int size,
			double lat, double lon, double distance)
			throws JsonProcessingException, IOException {
		QueryBuilder distanceQuery = QueryBuilders.geoDistanceQuery("location.geo")
				.distance(distance, DistanceUnit.KILOMETERS).point(lat, lon);
		QueryBuilder simpleQuery = QueryBuilders.queryStringQuery(q);
		QueryBuilder distanceAndSimpleQuery =
				QueryBuilders.boolQuery().must(distanceQuery).must(simpleQuery);
		SearchResponse queryResponse =
				executeQuery(from, size, distanceAndSimpleQuery);
		return returnAsJson(queryResponse);
	}

	static SearchResponse executeQuery(int from, int size, QueryBuilder query) {
		SearchResponse responseOfSearch = Index.CLIENT.prepareSearch(ES_NAME)
				.setTypes(ES_TYPE).setSearchType(SearchType.QUERY_THEN_FETCH)
				.setQuery(query).setFrom(from).setSize(size).execute().actionGet();
		return responseOfSearch;
	}

	private static String returnAsJson(SearchResponse queryResponse)
			throws IOException, JsonProcessingException {
		ImmutableMap<String, Object> meta =
				ImmutableMap.of("@id", "http://" + request().host() + request().uri(),
						"http://sindice.com/vocab/search#totalResults",
						queryResponse.getHits().getTotalHits());
		List<Map<String, Object>> hits =
				Arrays.asList(queryResponse.getHits().hits()).stream()
						.map(hit -> hit.getSource()).collect(Collectors.toList());
		hits.add(0, meta);
		return prettyJsonOk(Json.toJson(hits));
	}

	/**
	 * @param id The id of a document in the Elasticsearch index
	 * @param format The response format ('html' for HTML, else JSON)
	 * @return The source of a document as JSON
	 */
	public static Promise<Result> get(String id, String format) {
		response().setHeader("Access-Control-Allow-Origin", "*");
		String server =
				"http://localhost:" + CONFIG.getString("index.es.port.http");
		String url =
				String.format("%s/%s/%s/%s/_source", server, ES_NAME, ES_TYPE, id);
		return WS.url(url).execute().map(x -> x.getStatus() == OK
				? resultFor(id, x.asJson(), format) : notFound("Not found: " + id));
	}

	private static Status resultFor(String id, JsonNode json, String format)
			throws JsonProcessingException {
		return format != null && format.equals("html")
				? ok(organisation.render(id, json))
				: ok(prettyJsonOk(json)).as("application/json; charset=utf-8");
	}

	private static String prettyJsonOk(JsonNode jsonNode)
			throws JsonProcessingException {
		return new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(jsonNode);
	}
}