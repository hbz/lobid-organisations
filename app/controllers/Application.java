package controllers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;
import play.twirl.api.JavaScript;
import views.html.api;

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
	 * @return 200 ok response to render the index page
	 */
	public static Result index() {
		return ok(views.html.index.render());
	}

	/**
	 * @return 303 redirect to the referrer, after toggling the current language
	 */
	public static Result toggleLanguage() {
		changeLang(lang().code().startsWith("en") ? "de" : "en");
		return redirect(request().getHeader(REFERER));
	}

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
	 */
	public static Result search(String q, String location, int from, int size,
			String format) {
		try {
			String cacheKey =
					String.format("q=%s,location=%s,from=%s,size=%s,format=%s", q,
							location, from, size, format);
			Result cachedResult = (Result) Cache.get(cacheKey);
			if (cachedResult != null) {
				return cachedResult;
			}
			Result searchResult = searchResult(q, location, from, size, format);
			int oneDay = 60 * 60 * 24;
			Logger.debug("Caching search result for request: {}", cacheKey);
			Cache.set(cacheKey, searchResult, oneDay);
			return searchResult;
		} catch (IllegalArgumentException x) {
			x.printStackTrace();
			return badRequest("Bad request: " + x.getMessage());
		}
	}

	private static Result searchResult(String q, String location, int from,
			int size, String format) {
		if (q == null || q.isEmpty()) {
			return search("*", location, from, size, "html");
		}
		String queryResultString = searchQueryResult(q, location, from, size);
		Map<String, Supplier<Result>> results = new HashMap<>();
		results.put("html", () -> {
			String loc = location == null ? "" : location;
			Html html = views.html.search.render("lobid-organisations", q, loc,
					queryResultString, from, size);
			return ok(html).as("text/html; charset=utf-8");
		});
		results.put("js", () -> {
			String queryMetadata =
					Json.parse(queryResultString).iterator().next().toString();
			JavaScript script =
					views.js.facet_map.render(queryMetadata, q, location, from, size);
			return ok(script).as("application/javascript; charset=utf-8");
		});
		Supplier<Result> json = () -> {
			response().setHeader("Access-Control-Allow-Origin", "*");
			return ok(queryResultString).as("application/json; charset=utf-8");
		};
		Supplier<Result> resultSupplier = results.get(format);
		return resultSupplier == null ? json.get() : resultSupplier.get();
	}

	private static String searchQueryResult(String q, String location, int from,
			int size) {
		String result = null;
		if (location == null || location.isEmpty()) {
			result = buildSimpleQuery(q, from, size);
		} else {
			result = prepareLocationQuery(location, q, from, size);
		}
		return result;
	}

	private static String prepareLocationQuery(String location, String q,
			int from, int size) {
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
			String q, int from, int size) {
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
			String q, int from, int size) {
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

	private static String buildSimpleQuery(String q, int from, int size) {
		QueryBuilder simpleQuery = QueryBuilders.queryStringQuery(q);
		SearchResponse queryResponse = executeQuery(from, size, simpleQuery);
		return returnAsJson(queryResponse);
	}

	private static String buildPolygonQuery(String q, double[] latCoordinates,
			double[] lonCoordinates, int from, int size) {
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
			double lat, double lon, double distance) {
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
		SearchRequestBuilder searchRequest = Index.CLIENT.prepareSearch(ES_NAME)
				.setTypes(ES_TYPE).setSearchType(SearchType.QUERY_THEN_FETCH)
				.setQuery(query).setFrom(from).setSize(size);
		searchRequest =
				withAggregations(searchRequest, "type.raw", "classification.label.raw",
						"fundertype.label.raw", "stocksize.label.raw");
		return searchRequest.execute().actionGet();
	}

	private static SearchRequestBuilder withAggregations(
			final SearchRequestBuilder searchRequest, String... fields) {
		Arrays.asList(fields).forEach(field -> {
			searchRequest
					.addAggregation(AggregationBuilders.terms(field.replace(".raw", ""))
							.field(field).size(Integer.MAX_VALUE));
		});
		searchRequest.addAggregation(AggregationBuilders.terms("location.geo")
				.script(new Script("location-aggregation")).size(Integer.MAX_VALUE));
		return searchRequest;
	}

	private static String returnAsJson(SearchResponse queryResponse) {
		List<Map<String, Object>> hits =
				Arrays.asList(queryResponse.getHits().hits()).stream()
						.map(hit -> hit.getSource()).collect(Collectors.toList());
		hits.add(0, queryMetadata(queryResponse));
		return prettyJsonOk(Json.toJson(hits));
	}

	private static Map<String, Object> queryMetadata(
			SearchResponse queryResponse) {
		return ImmutableMap.<String, Object> builder()
				.put("@id", "http://" + request().host() + request().uri())
				.put("http://sindice.com/vocab/search#totalResults",
						queryResponse.getHits().getTotalHits())
				.put("aggregations",
						Json.parse(queryResponse.toString()).get("aggregations"))
				.build();
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

	private static Result resultFor(String id, JsonNode json, String format) {
		Map<String, Supplier<Result>> results = new HashMap<>();
		results.put("html",
				() -> ok(views.html.organisation.render(id, json,
						json.findValue("location") != null))
								.as("text/html; charset=utf-8"));
		results.put("js",
				() -> ok(views.js.location_details.render(json.toString()))
						.as("application/javascript; charset=utf-8"));
		Supplier<Result> jsonSupplier =
				() -> ok(prettyJsonOk(json)).as("application/json; charset=utf-8");
		Supplier<Result> resultSupplier = results.get(format);
		return resultSupplier == null ? jsonSupplier.get() : resultSupplier.get();
	}

	private static String prettyJsonOk(JsonNode jsonNode) {
		try {
			return new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(jsonNode);
		} catch (JsonProcessingException x) {
			x.printStackTrace();
			return null;
		}
	}
}
