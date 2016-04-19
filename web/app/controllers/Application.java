package controllers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Play;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

/**
 * 
 * Contains the methods of the play application to search, get source, and
 * display context
 *
 */
public class Application extends Controller {

	private static final String SERVER_NAME = "localhost";
	private static final String ES_SERVER = "http://" + SERVER_NAME + ":9200";
	private static final String ES_INDEX = "organisations";
	private static final String ES_TYPE = "organisation";

	private static Settings clientSettings =
			Settings.settingsBuilder().put("cluster.name", "elasticsearch")
					.put("client.transport.ping_timeout", 20, TimeUnit.SECONDS).build();
	private static TransportClient transportClient =
			TransportClient.builder().settings(clientSettings).build();
	private static InetSocketTransportAddress node =
			new InetSocketTransportAddress(new InetSocketAddress(SERVER_NAME, 9300));
	private static Client client = transportClient.addTransportAddress(node);

	/**
	 * @return 200 ok response to render index
	 */
	public static Result index() {
		return ok(index.render("lobid-organisations"));
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
	 * @return Result of search as ok() or badRequest()
	 * @throws JsonProcessingException Is thrown if response of search cannot be
	 *           processed as JsonNode
	 * @throws IOException Is thrown if response of search cannot be processed as
	 *           JsonNode
	 */
	public static Result search(String q, String location, int from, int size)
			throws JsonProcessingException, IOException {
		response().setHeader("Access-Control-Allow-Origin", "*");
		Status result = null;
		if (location == null) {
			result = buildSimpleQuery(q, from, size);
		} else {
			result = prepareLocationQuery(location, q, from, size);
		}
		return result;
	}

	private static Status prepareLocationQuery(String location, String q,
			int from, int size) throws JsonProcessingException, IOException {
		String[] coordPairsAsString = location.split(" ");
		Status result;
		if (coordPairsAsString[0].split(",").length > 2) {
			result = prepareDistanceQuery(coordPairsAsString, q, from, size);
		} else {
			result = preparePolygonQuery(coordPairsAsString, q, from, size);
		}
		return result;
	}

	private static Status preparePolygonQuery(String[] coordPairsAsString,
			String q, int from, int size)
					throws JsonProcessingException, IOException {
		double[] latCoordinates = new double[coordPairsAsString.length];
		double[] lonCoordinates = new double[coordPairsAsString.length];
		Status result;
		for (int i = 0; i < coordPairsAsString.length; i++) {
			String[] coordinatePair = coordPairsAsString[i].split(",");
			latCoordinates[i] = Double.parseDouble(coordinatePair[0]);
			lonCoordinates[i] = Double.parseDouble(coordinatePair[1]);
		}
		if (coordPairsAsString.length < 3) {
			return badRequest(
					"Not enough points. Polygon requires more than two points.");
		}
		result = buildPolygonQuery(q, latCoordinates, lonCoordinates, from, size);
		return result;
	}

	private static Status prepareDistanceQuery(String[] coordPairsAsString,
			String q, int from, int size)
					throws JsonProcessingException, IOException {
		String[] coordinatePair = coordPairsAsString[0].split(",");
		double lat = Double.parseDouble(coordinatePair[0]);
		double lon = Double.parseDouble(coordinatePair[1]);
		double distance = Double.parseDouble(coordinatePair[2]);
		Status result;
		if (distance < 0) {
			return badRequest("Distance must not be smaller than 0.");
		}
		result = buildDistanceQuery(q, from, size, lat, lon, distance);
		return result;
	}

	private static Status buildSimpleQuery(String q, int from, int size)
			throws JsonProcessingException, IOException {
		QueryBuilder simpleQuery = QueryBuilders.queryStringQuery(q);
		SearchResponse queryResponse = executeQuery(from, size, simpleQuery);
		return returnAsJson(queryResponse);
	}

	private static Status buildPolygonQuery(String q, double[] latCoordinates,
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

	private static Status buildDistanceQuery(String q, int from, int size,
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
		SearchResponse responseOfSearch = client.prepareSearch(ES_INDEX)
				.setTypes(ES_TYPE).setSearchType(SearchType.QUERY_THEN_FETCH)
				.setQuery(query).setFrom(from).setSize(size).execute().actionGet();
		return responseOfSearch;
	}

	private static Status returnAsJson(SearchResponse queryResponse)
			throws IOException, JsonProcessingException {
		return prettyJsonOk(new ObjectMapper().readTree(queryResponse.toString()));
	}

	/**
	 * @param id The id of a document in the Elasticsearch index
	 * @return The source of a document as JSON
	 */
	public static Promise<Result> get(String id) {
		response().setHeader("Access-Control-Allow-Origin", "*");
		String url =
				String.format("%s/%s/%s/%s/_source", ES_SERVER, ES_INDEX, ES_TYPE, id);
		return WS.url(url).execute().map(x -> x.getStatus() == OK
				? prettyJsonOk(x.asJson()) : notFound("Not found: " + id));
	}

	private static Status prettyJsonOk(JsonNode jsonNode)
			throws JsonProcessingException {
		return ok(new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(jsonNode)).as("application/json; charset=utf-8");
	}
}