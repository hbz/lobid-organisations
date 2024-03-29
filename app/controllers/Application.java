/* Copyright 2014-2016, hbz. Licensed under the EPL 2.0 */

package controllers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import controllers.RdfConverter.RdfFormat;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import play.twirl.api.JavaScript;
import scala.util.Random;
import transformation.CsvExport;
import views.html.api;
import views.html.dataset;

/**
 * 
 * Contains the methods of the play application to search, get source, and
 * display context
 *
 */
public class Application extends Controller {

	private static final String JSON_UTF8 = "application/json; charset=utf-8";

	private static final int ONE_DAY = 60 * 60 * 24;

	static final String FORMAT_CONFIG_SEP = ":";

	/** The application config. */
	public static final Config CONFIG = ConfigFactory.load();

	/**
	 * @param path The path to redirect to
	 * @return A 301 MOVED_PERMANENTLY redirect to the path
	 */
	public static Result redirect(String path) {
		return movedPermanently("/" + path);
	}

	/**
	 * @return 200 ok response to render the index page
	 */
	public static Promise<Result> index() {
		try {
			return requestImages().flatMap(images -> {
				JsonNode image = pickRandomItem(images);
				String label = labelFor(image);
				String imageUrl = image.get("image").get("value").asText();
				String imageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
				return requestInfo(imageName).map(info -> {
					String attribution = createAttribution(imageName, info.asJson());
					JsonNode dataset = Json.parse(readFile("dataset"));
					return (Result) ok(
							views.html.index.render(dataset, image, attribution, label));
				});
			}).recoverWith(t -> index());
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
			return Promise.pure(internalServerError(e.getMessage()));
		}
	}

	private static JsonNode pickRandomItem(WSResponse images) {
		Iterator<JsonNode> elements =
				images.asJson().findValue("bindings").elements();
		List<JsonNode> items = new ArrayList<>();
		elements.forEachRemaining(items::add);
		return items.get(new Random().nextInt(items.size()));
	}

	private static String labelFor(JsonNode image) {
		String isil = image.get("isil").get("value").asText();
		String locality = Optional.ofNullable(Index.get(isil))
				.map(organisation -> Json.parse(organisation).findValue("location"))
				.map(location -> location.findValue("addressLocality"))
				.map(JsonNode::asText).orElseGet(() -> "");
		String imageLabel = image.get("itemLabel").get("value").asText();
		return !locality.isEmpty() && !imageLabel.contains(locality)
				? imageLabel + ", " + locality : imageLabel;
	}

	private static Promise<WSResponse> requestImages() throws IOException {
		File sparqlFile = Play.application().getFile("conf/wikidata.sparql");
		String sparqlString = Files.readAllLines(Paths.get(sparqlFile.toURI()))
				.stream().collect(Collectors.joining("\n"));
		return cachedRequest(sparqlString,
				WS.url("https://query.wikidata.org/sparql")
						.setQueryParameter("query", sparqlString)
						.setQueryParameter("format", "json"));
	}

	private static Promise<WSResponse> requestInfo(String imageName)
			throws UnsupportedEncodingException {
		String imageId =
				"File:" + URLDecoder.decode(imageName, StandardCharsets.UTF_8.name());
		return cachedRequest(imageId,
				WS.url("https://commons.wikimedia.org/w/api.php")
						.setQueryParameter("action", "query")
						.setQueryParameter("format", "json")
						.setQueryParameter("prop", "imageinfo")
						.setQueryParameter("iiprop", "extmetadata")
						.setQueryParameter("titles", imageId));
	}

	private static Promise<WSResponse> cachedRequest(String key,
			WSRequestHolder request) {
		@SuppressWarnings("unchecked")
		Promise<WSResponse> promise = (Promise<WSResponse>) Cache.get(key);
		if (promise == null) {
			promise = request.get();
			promise.onRedeem(response -> {
				if (response.getStatus() == Http.Status.OK) {
					Cache.set(key, Promise.pure(response), ONE_DAY);
				}
			});
		}
		return promise;
	}

	private static String createAttribution(String fileName, JsonNode info) {
		String artist = findText(info, "Artist");
		String licenseText = findText(info, "LicenseShortName");
		String licenseUrl = findText(info, "LicenseUrl");
		String fileSourceUrl =
				"https://commons.wikimedia.org/wiki/File:" + fileName;
		return String.format(
				(artist.isEmpty() ? "%s" : "%s, ")
						+ "<a href='%s'>Wikimedia Commons</a>, <a href='%s'>%s</a>",
				artist, fileSourceUrl,
				licenseUrl.isEmpty() ? fileSourceUrl : licenseUrl, licenseText);
	}

	private static String findText(JsonNode info, String field) {
		JsonNode node = info.findValue(field);
		return node != null ? node.get("value").asText().replace("\n", " ") : "";
	}

	/**
	 * @return 303 redirect to the referrer, after toggling the current language
	 */
	public static Result toggleLanguage() {
		changeLang(isEnglish() ? "de" : "en");
		return seeOther(request().getHeader(REFERER).replaceAll("en|de", ""));
	}

	/**
	 * @return The current language, "en" or "de"
	 */
	public static String currentLang() {
		return isEnglish() ? "en" : "de";
	}

	/**
	 * @return The current full URI, URL-encoded, or null.
	 */
	public static String currentUri() {
		try {
			return URLEncoder.encode(request().host() + request().uri(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error("Could not get current URI", e);
		}
		return null;
	}

	/**
	 * @param field The field name
	 * @param organisation The organisation JSON
	 * @return The localized field name to use for the given organisation (if the
	 *         data contains it), or the given field name
	 */
	public static String localizedOptional(String field, JsonNode organisation) {
		final String localizedFieldName = field + "_en";
		return isEnglish() && organisation.has(localizedFieldName)
				? localizedFieldName : field;
	}

	/**
	 * @param field The full field to localize
	 * @return The full field, with the `label` changed to `label.en` or
	 *         `label.de`
	 */
	public static String localizedLabel(String field) {
		return field.replace("label", isEnglish() ? "label.en" : "label.de");
	}

	private static boolean isEnglish() {
		return lang().code().split("-")[0].equals("en");
	}

	/**
	 * @return Redirect to the localized version of the API documentation
	 */
	public static Result api() {
		return redirect(routes.Application.apiLocalized(currentLang()));
	}

	/**
	 * @return 200 ok response to render api documentation
	 * @param lang The language for the documentation ('de' or 'en')
	 */
	public static Result apiLocalized(String lang) {
		if (!Arrays.asList("de", "en").contains(lang)) {
			return badRequest("Unsupported lang: " + lang);
		}
		changeLang(lang);
		return ok(api.render("lobid-organisations"));
	}

	/**
	 * @return JSON-LD context
	 */
	public static Result context() {
		return staticJsonld("context");
	}

	/**
	 * See https://www.w3.org/TR/dwbp/#metadata
	 * 
	 * @return JSON-LD dataset metadata
	 */
	public static Result dataset() {
		return staticJsonld("dataset");
	}

	private static Result staticJsonld(String name) {
		response().setContentType("application/ld+json");
		response().setHeader("Access-Control-Allow-Origin", "*");
		return ok(readFile(name));
	}

	/**
	 * See https://www.w3.org/TR/dwbp/#metadata
	 * 
	 * @param format The format ("json" or "html")
	 * 
	 * @return JSON-LD dataset metadata
	 */
	public static Result dataset(String format) {
		String responseFormat = Accept.formatFor(format, request().acceptedTypes());
		return responseFormat.matches(Accept.Format.JSON_LD.queryParamString)
				? staticJsonld("dataset")
				: ok(dataset.render(Json.parse(readFile("dataset"))));
	}

	private static String readFile(String name) {
		try {
			return Streams
					.readAllLines(Play.application().resourceAsStream(name + ".jsonld"))
					.stream().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			Logger.error("Couldn't get dataset", e);
			return null;
		}
	}

	/**
	 * @param q The search string
	 * @param location The geographical location to search in (polygon points or
	 *          single point plus distance)
	 * @param from From parameter for Elasticsearch query
	 * @param size Size parameter for Elasitcsearch query
	 * @param format The response format ('html' for HTML, else JSON)
	 * @param aggregationsParam The comma separated aggregation fields
	 * @return Result of search as ok() or badRequest()
	 */
	public static Result search(String q, String location, int from, int size,
			String format, String aggregationsParam) {
		String aggregations = aggregationsParam;
		if (!aggregations.isEmpty()) {
			aggregations = Arrays.asList(aggregations.split(",")).stream()
					.map((a) -> a.endsWith(".raw") ? a : a + ".raw")
					.collect(Collectors.joining(","));
			if (!Index.SUPPORTED_AGGREGATIONS
					.containsAll(Arrays.asList(aggregations.split(",")))) {
				return badRequest(
						String.format("Unsupported aggregations: %s (supported: %s)",
								aggregations.replace(".raw", ""), Index.SUPPORTED_AGGREGATIONS
										.toString().replace(".raw", "")));
			}
		}
		final String responseFormat =
				Accept.formatFor(format, request().acceptedTypes());
		boolean isBulkRequest =
				responseFormat.equals(Accept.Format.BULK.queryParamString);
		if (isBulkRequest) {
			response().setHeader("Content-Disposition",
					String.format(
							"attachment; filename=\"lobid-organisations-bulk-%s.jsonl\"",
							System.currentTimeMillis()));
		}
		try {
			String cacheKey = String.format(
					"q=%s,location=%s,from=%s,size=%s,format=%s,lang=%s,position=%s", q,
					location, from, size, responseFormat, lang().code(),
					session("position"));
			Result cachedResult = (Result) Cache.get(cacheKey);
			if (cachedResult != null && responseFormat.equals("html")) {
				return cachedResult;
			}
			Result searchResult = isBulkRequest ? bulkResult(q)
					: searchResult(q, location, from, size, responseFormat, aggregations);
			Logger.debug("Caching search result for request: {}", cacheKey);
			Cache.set(cacheKey, searchResult, ONE_DAY);
			return searchResult;
		} catch (Throwable t) {
			String message = t.getMessage()
					+ (t.getCause() != null ? ", cause: " + t.getCause().getMessage()
							: "");
			Logger.error("Error: {}", message);
			return internalServerError(
					views.html.error.render(q, "Error: " + message));
		}
	}

	private static Result bulkResult(final String q) {
		Chunks<String> chunks = StringChunks.whenReady(out -> {
			Client client = Index.CLIENT;
			QueryBuilder query = QueryBuilders.queryStringQuery(q);
			Logger.trace("bulkOrganisations: q={}, query={}", q, query);
			TimeValue keepAlive = new TimeValue(60000);
			SearchResponse scrollResp = client.prepareSearch(Index.INDEX_NAME)
					.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
					.setScroll(keepAlive).setQuery(query)
					.setSize(100 /* hits per shard for each scroll */).get();
			String scrollId = scrollResp.getScrollId();
			while (scrollResp.getHits().iterator().hasNext()) {
				scrollResp.getHits().forEach((hit) -> {
					out.write(hit.getSourceAsString());
					out.write("\n");
				});
				scrollResp = client.prepareSearchScroll(scrollId).setScroll(keepAlive)
						.execute().actionGet();
				scrollId = scrollResp.getScrollId();
			}
			out.close();
		});
		return ok(chunks).as(Accept.Format.BULK.types[0]);
	}

	private static Result searchResult(String q, String location, int from,
			int size, String format, String aggregations) {
		if (q == null || q.isEmpty()) {
			return search("*", location, from, size, "html", aggregations);
		}
		Map<String, Supplier<Result>> results = new HashMap<>();
		results.put("html", () -> {
			String queryResultString = searchQueryResult(q, location, from, size,
					Joiner.on(",").join(defaultAggregations()));
			String loc = location == null ? "" : location;
			Html html = views.html.search.render("lobid-organisations", q, loc,
					queryResultString, from, size);
			return ok(html).as("text/html; charset=utf-8");
		});
		results.put("js", () -> {
			String queryResultString =
					searchQueryResult(q, location, from, size, "location");
			JsonNode queryMetadata = Json.parse(queryResultString).get("aggregation");
			JavaScript script = views.js.facet_map.render(
					queryMetadata == null ? "{}" : queryMetadata.toString(), q, location,
					from, size);
			return ok(script).as("application/javascript; charset=utf-8");
		});
		results.put("csv", () -> {
			String queryResultString =
					searchQueryResult(q, location, from, size, aggregations);
			String orgs = Json.parse(queryResultString).get("member").toString();
			response().setHeader("Content-Disposition",
					"attachment; filename=organisations.csv");
			return ok(csvExport(format, orgs)).as("text/csv; charset=utf-8");
		});
		results.put("tsv", () -> {
			String queryResultString =
					searchQueryResult(q, location, from, size, aggregations);
			String orgs = Json.parse(queryResultString).get("member").toString();
			response().setHeader("Content-Disposition",
					"attachment; filename=organisations.tsv");
			return ok(csvExport(format, orgs, CsvExport.TAB_SEPARATOR)).as("text/tab-separated-values; charset=utf-8");
		});
		Supplier<Result> json = () -> {
			String queryResultString =
					searchQueryResult(q, location, from, size, aggregations);
			String[] formatAndConfig = format.split(FORMAT_CONFIG_SEP);
			boolean returnSuggestions = formatAndConfig.length == 2;
			if (returnSuggestions) {
				queryResultString =
						toSuggestions(queryResultString, formatAndConfig[1]);
			}
			response().setHeader("Access-Control-Allow-Origin", "*");
			return returnSuggestions ? withCallback(queryResultString)
					: ok(queryResultString).as(JSON_UTF8);
		};
		Supplier<Result> resultSupplier =
				results.get(format.split(FORMAT_CONFIG_SEP)[0]);
		return resultSupplier == null ? json.get() : resultSupplier.get();
	}

	private static Status withCallback(final String json) {
		/* JSONP callback support for remote server calls with JavaScript: */
		final String[] callback =
				request() == null || request().queryString() == null ? null
						: request().queryString().get("callback");
		return callback != null ? ok(String.format("/**/%s(%s)", callback[0], json))
				.as("application/javascript; charset=utf-8") : ok(json).as(JSON_UTF8);
	}

	private static String toSuggestions(String json, String field) {
		Stream<JsonNode> documents =
				StreamSupport.stream(Spliterators.spliteratorUnknownSize(
						Json.parse(json).get("member").elements(), 0), false);
		Stream<JsonNode> suggestions = documents.flatMap((JsonNode document) -> {
			Stream<JsonNode> nodes = fieldValues(field, document);
			return nodes.map((JsonNode node) -> {
				boolean isTextual = node.isTextual();
				Optional<JsonNode> label = isTextual ? Optional.ofNullable(node)
						: findValueOptional(node, "label");
				Optional<JsonNode> id = isTextual ? getOptional(document, "id")
						: findValueOptional(node, "id");
				Optional<JsonNode> type = isTextual ? getOptional(document, "type")
						: findValueOptional(node, "type");
				String typeText = type.orElseGet(() -> Json.toJson("")).textValue();
				return Json.toJson(ImmutableMap.of(//
						"label", label.orElseGet(() -> Json.toJson("")), //
						"id", id.orElseGet(() -> label.orElseGet(() -> Json.toJson(""))), //
						"category", typeText));
			});
		});
		return Json.toJson(suggestions.collect(Collectors.toSet())).toString();
	}

	private static Stream<JsonNode> fieldValues(String field, JsonNode document) {
		return document.findValues(field).stream().flatMap((node) -> {
			return node.isArray()
					? StreamSupport.stream(
							Spliterators.spliteratorUnknownSize(node.elements(), 0), false)
					: Arrays.asList(node).stream();
		});
	}

	private static Optional<JsonNode> findValueOptional(JsonNode json,
			String field) {
		return Optional.ofNullable(json.findValue(field));
	}

	private static Optional<JsonNode> getOptional(JsonNode json, String field) {
		return Optional.ofNullable(json.get(field));
	}

	private static String csvExport(String format, String orgs, String separator) {
		String[] formatConfig = format.split(FORMAT_CONFIG_SEP); // e.g. csv:name,id
		String fields = formatConfig.length > 1 && !formatConfig[1].isEmpty()
				? formatConfig[1] : defaultFields();
		if (separator == null) {
			return new CsvExport(orgs).of(fields);
		}
		else {
			String fieldsWithNonDefaultSeparator=fields.replaceAll(",", separator);
			return new CsvExport(orgs).of(fieldsWithNonDefaultSeparator, separator);
		}
	}

	private static String csvExport(String format, String orgs) {
		return csvExport(format, orgs, CsvExport.DEFAULT_SEPARATOR);
	}

	private static String defaultFields() {
		return "name,name_en,id,isil,dbsID,type,rs,url,wikipedia,telephone,email,"
				+ "address.postalCode,address.addressLocality,address.addressCountry,"
				+ "location[0].geo.lat,location[0].geo.lon,"
				+ "location[0].address.streetAddress,location[0].address.postalCode,"
				+ "location[0].address.addressLocality,location[0].address.addressCountry,"
				+ "classification.id," + localizedLabel("classification.label")
				+ ",fundertype.id," + localizedLabel("fundertype.label")
				+ ",stocksize.id," + localizedLabel("stocksize.label")
				+ ",alternateName[0],alternateName[1]";
	}

	private static String searchQueryResult(String q, String location, int from,
			int size, String aggregations) {
		String result;
		if (location == null || location.isEmpty()) {
			result = buildSimpleQuery(q, from, size, aggregations);
		} else {
			result = prepareLocationQuery(location, q, from, size, aggregations);
		}
		return result;
	}

	private static String prepareLocationQuery(String location, String q,
			int from, int size, String aggregations) {
		String[] coordPairsAsString = location.split(" ");
		String result;
		if (coordPairsAsString[0].split(",").length > 2) {
			result =
					prepareDistanceQuery(coordPairsAsString, q, from, size, aggregations);
		} else {
			result =
					preparePolygonQuery(coordPairsAsString, q, from, size, aggregations);
		}
		return result;
	}

	private static String preparePolygonQuery(String[] coordPairsAsString,
			String q, int from, int size, String aggregations) {
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
		result = buildPolygonQuery(q, latCoordinates, lonCoordinates, from, size,
				aggregations);
		return result;
	}

	private static String prepareDistanceQuery(String[] coordPairsAsString,
			String q, int from, int size, String aggregations) {
		String[] coordinatePair = coordPairsAsString[0].split(",");
		double lat = Double.parseDouble(coordinatePair[0]);
		double lon = Double.parseDouble(coordinatePair[1]);
		double distance = Double.parseDouble(coordinatePair[2]);
		String result;
		if (distance < 0) {
			throw new IllegalArgumentException(
					"Distance must not be smaller than 0.");
		}
		result =
				buildDistanceQuery(q, from, size, lat, lon, distance, aggregations);
		return result;
	}

	private static String buildSimpleQuery(String q, int from, int size,
			String aggregations) {
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(mainQuery(q))
				.must(QueryBuilders.existsQuery("type"));
		SearchResponse queryResponse =
				Index.executeQuery(from, size, boolQuery, aggregations);
		return returnAsJson(queryResponse);
	}

	private static String buildPolygonQuery(String q, double[] latCoordinates,
			double[] lonCoordinates, int from, int size, String aggregations) {
		GeoPolygonQueryBuilder polygonQuery =
				QueryBuilders.geoPolygonQuery(Index.GEO_FIELD);
		for (int i = 0; i < latCoordinates.length; i++) {
			polygonQuery.addPoint(latCoordinates[i], lonCoordinates[i]);
		}
		QueryBuilder polygonAndSimpleQuery =
				QueryBuilders.boolQuery().must(polygonQuery).must(mainQuery(q));
		SearchResponse queryResponse =
				Index.executeQuery(from, size, polygonAndSimpleQuery, aggregations);
		return returnAsJson(queryResponse);
	}

	private static String buildDistanceQuery(String q, int from, int size,
			double lat, double lon, double distance, String aggregations) {
		QueryBuilder distanceQuery = QueryBuilders.geoDistanceQuery(Index.GEO_FIELD)
				.distance(distance, DistanceUnit.KILOMETERS).point(lat, lon);
		QueryBuilder distanceAndSimpleQuery =
				QueryBuilders.boolQuery().must(distanceQuery).must(mainQuery(q));
		SearchResponse queryResponse =
				Index.executeQuery(from, size, distanceAndSimpleQuery, aggregations);
		return returnAsJson(queryResponse);
	}

	private static QueryBuilder mainQuery(String q) {
		QueryBuilder stringQuery = QueryBuilders.queryStringQuery(q);
		return !q.contains("type:Collection")
				? QueryBuilders.boolQuery().must(stringQuery).mustNot(
						QueryBuilders.matchQuery("type", "Collection"))
				: stringQuery;
	}

	static String[] defaultAggregations() {
		return new String[] { "type.raw",
				localizedLabel("classification.label.raw"),
				localizedLabel("fundertype.label.raw"),
				localizedLabel("collects.extent.label.raw"), "location" };
	}

	private static String returnAsJson(SearchResponse queryResponse) {
		List<Map<String, Object>> hits =
				Arrays.stream(queryResponse.getHits().hits())
						.map(SearchHit::getSource).collect(Collectors.toList());
		ObjectNode object = Json.newObject();
		object.put("@context",
				CONFIG.getString("host") + routes.Application.context());
		object.put("id", CONFIG.getString("host") + request().uri());
		object.put("totalItems", queryResponse.getHits().getTotalHits());
		object.set("member", Json.toJson(hits));
		JsonNode aggregations =
				Json.parse(queryResponse.toString()).get("aggregations");
		if (aggregations != null) {
			object.set("aggregation", aggregations);
		}
		return prettyJsonOk(object);
	}

	/**
	 * @param id The id of a document in the Elasticsearch index
	 * @param format The response format (see {@code Accept.Format})
	 * @return The result document in the requested format
	 */
	public static Promise<Result> getDotFormat(String id, String format) {
		return get(id, format);
	}

	/**
	 * @param id The id of a document in the Elasticsearch index
	 * @param format The response format (see {@code Accept.Format})
	 * @return The result document in the requested format
	 */
	public static Promise<Result> get(String id, String format) {
		final String responseFormat =
				Accept.formatFor(format, request().acceptedTypes());
		response().setHeader("Access-Control-Allow-Origin", "*");
		Optional<Result> result = Optional.ofNullable(Index.get(id))
				.map(json -> resultFor(id, Json.parse(json), responseFormat));
		if (!result.isPresent()) {
			SearchResponse response = Index.executeQuery(0, 1,
					QueryBuilders.matchQuery("dbsID", id.replace("DBS-", "")), "");
			if (response.getHits().getTotalHits() > 0) {
				result =
						Optional.ofNullable(response.getHits().getAt(0).getSourceAsString())
								.map(json -> Json.parse(json).get("isil"))
								.map(isil -> movedPermanently(
										routes.Application.get(isil.asText(), format)));
			}
		}
		return Promise.pure(result.orElseGet(() -> notFound(
				views.html.organisation.render(id, Json.parse("{}"), false))));
	}

	/**
	 * @param lat The position's latitude
	 * @param lon The position's longitude
	 * @return 200 OK, after storing the position in the session
	 */
	public static Result setPosition(String lat, String lon) {
		session("position", lat + "," + lon);
		return ok("Position set to " + session("position"));
	}

	/**
	 * @return @return 303 redirect to the referrer, after removing the position
	 *         from the session
	 */
	public static Result removePosition() {
		session().remove("position");
		return seeOther(request().getHeader(REFERER));
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
		results.put("csv", () -> {
			response().setHeader("Content-Disposition",
					String.format("attachment; filename=%s.csv", id));
			return ok(csvExport(format, "[" + json.toString() + "]"))
					.as("text/csv; charset=utf-8");
		});
		results.put("tsv", () -> {
			response().setHeader("Content-Disposition",
					String.format("attachment; filename=%s.tsv", id));
			return ok(csvExport(format, "[" + json.toString() + "]", CsvExport.TAB_SEPARATOR))
					.as("text/tab-separated-values; charset=utf-8");
		});
		Pair<String, String> contentAndType = contentAndType(json, format);
		Supplier<Result> rdfSupplier =
				() -> ok(contentAndType.getLeft()).as(contentAndType.getRight());
		Supplier<Result> resultSupplier =
				results.get(format.split(FORMAT_CONFIG_SEP)[0]);
		return resultSupplier == null ? rdfSupplier.get() : resultSupplier.get();
	}

	private static Pair<String, String> contentAndType(JsonNode responseJson,
			String responseFormat) {
		String content;
		String contentType;
		switch (responseFormat) {
		case "rdf": {
			content = RdfConverter.toRdf(responseJson.toString(), RdfFormat.RDF_XML);
			contentType = Accept.Format.RDF_XML.types[0];
			break;
		}
		case "ttl": {
			content = RdfConverter.toRdf(responseJson.toString(), RdfFormat.TURTLE);
			contentType = Accept.Format.TURTLE.types[0];
			break;
		}
		case "nt": {
			content = RdfConverter.toRdf(responseJson.toString(), RdfFormat.N_TRIPLE);
			contentType = Accept.Format.N_TRIPLE.types[0];
			break;
		}
		default: {
			content = prettyJsonOk(responseJson);
			contentType = Accept.Format.JSON_LD.types[0];
		}
		}
		return Pair.of(content, contentType + "; charset=utf-8");
	}

	private static String prettyJsonOk(JsonNode jsonNode) {
		try {
			return new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(jsonNode);
		} catch (JsonProcessingException x) {
			Logger.warn(x.getMessage());
			return null;
		}
	}
}
