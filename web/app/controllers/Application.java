package controllers;


import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceFilterBuilder;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.MatchAllFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.*;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {

	private static final String ES_SERVER = "http://weywot2.hbz-nrw.de:9200";
	private static final String ES_INDEX = "organisations";
	private static final String ES_TYPE = "dbs";
	
	private static Settings clientSettings =
			ImmutableSettings.settingsBuilder()
					.put("cluster.name", "organisation-cluster")
					.put("client.transport.sniff", true).build();
	private static TransportClient transportClient = new TransportClient(clientSettings);
	private static Client client =
			transportClient.addTransportAddress(new InetSocketTransportAddress(
					"weywot2.hbz-nrw.de", 9300));

	public static Result index() {
		return ok(index.render("lobid-organisations"));
	}

	public static Result context() {
		response().setContentType("application/ld+json");
		response().setHeader("Access-Control-Allow-Origin", "*");
		return ok(Play.application().resourceAsStream("context.jsonld"));
	}
	
	public static Result search(String q, String location, double distance, int from, int size) throws JsonProcessingException, IOException {
		String[] queryParts = q.split(":");
		String field = queryParts[0];
		String term = queryParts[1];		
		SearchResponse queryResponse;
		
		if(location == null) {
			queryResponse = querySimple(field, term, from, size);			
		} else {
			if (distance == -1){
				String[] coordinatesAsString = location.split("[,+]");
				double[] coordinates = new double[coordinatesAsString.length];
				for (int i = 0; i < coordinatesAsString.length; i++){
					coordinates[i] = Double.parseDouble(coordinatesAsString[i]);
				}
				queryResponse = queryPolygon(field,term,coordinates, from, size);
			} else {
				String[] coordinatesAsString = location.split("[,+]");
				double lat = Double.parseDouble(coordinatesAsString[0]);
				double lon = Double.parseDouble(coordinatesAsString[1]);
				queryResponse = queryDistance(field, term, from, size, lat, lon, distance);
			}
		}
		JsonNode responseAsJson = new ObjectMapper().readTree(queryResponse.toString());
		return ok(responseAsJson);
	}
	
	private static SearchResponse querySimple(String field, String term, int from, int size) {		
		MatchAllFilterBuilder matchAllFilter = FilterBuilders.matchAllFilter();
		FilteredQueryBuilder simpleQuery =
				QueryBuilders.filteredQuery(QueryBuilders.matchQuery(field, term), matchAllFilter);
		return executeQuery(from, size, simpleQuery);
	}
	
	private static SearchResponse queryPolygon(String field, String term, double[] coordinates, int from, int size) {
		GeoPolygonFilterBuilder polygonFilter =
				FilterBuilders.geoPolygonFilter("location").addPoint(coordinates[0], coordinates[1])
						.addPoint(coordinates[2], coordinates[3]).addPoint(coordinates[4], coordinates[5])
						.addPoint(coordinates[6], coordinates[7]);
		FilteredQueryBuilder polygonQuery =
				QueryBuilders.filteredQuery(QueryBuilders.matchQuery(field, term), polygonFilter);
		return executeQuery(from, size, polygonQuery);
	}
	
	private static SearchResponse queryDistance(String field, String term, int from, int size, double lat, double lon, double distance){
		GeoDistanceFilterBuilder distanceFilter =
				FilterBuilders.geoDistanceFilter("location").distance(distance, DistanceUnit.KILOMETERS).point(lat, lon);
		FilteredQueryBuilder distanceQuery =
				QueryBuilders.filteredQuery(QueryBuilders.matchQuery(field, term), distanceFilter);
		return executeQuery(from, size, distanceQuery);
	}

	private static SearchResponse executeQuery(int from, int size, FilteredQueryBuilder filteredQuery) {
		SearchResponse responseOfSearch =
				client.prepareSearch("organisations").setTypes("dbs")
						.setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(filteredQuery)
						.setFrom(from).setSize(size)
						.execute().actionGet();
		return responseOfSearch;
	}

	public static Promise<Result> get(String id) {
		String url = String.format("%s/%s/%s/%s/_source", ES_SERVER, ES_INDEX, ES_TYPE, id);
		return WS.url(url).execute().map(x -> ok(x.asJson()));
	}
}