package controllers;


import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.*;
import play.api.mvc.AnyContent;
import play.api.mvc.Request;
import play.cache.Cached;
import play.libs.F.Promise;
import play.libs.Json;
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

	// public static Promise<Result> search(String q, String location) {
	public static Result search(String q, String location) throws IOException {
		// String url = String.format("%s/%s/_search", ES_SERVER, ES_INDEX);
		
		String[] queryParts = q.split(":");
		String field = queryParts[0];
		String term = queryParts[1];
		
		String[] coordinatesAsString = location.split("[,+]");
		double[] coordinates = new double[coordinatesAsString.length];		
		for (int i=0; i<coordinatesAsString.length; i++){
			coordinates[i] = Double.parseDouble(coordinatesAsString[i]);
		}		
		SearchResponse queryResponse = query(field,term, coordinates);
		JsonNode responseAsJson = new ObjectMapper().readTree(queryResponse.toString());
		
		return ok(responseAsJson);		
		// return WS.url(url).setQueryParameter("q", q).execute().map(x -> ok(x.asJson()));
	}

	public static Promise<Result> get(String id) {
		String url = String.format("%s/%s/%s/%s/_source", ES_SERVER, ES_INDEX, ES_TYPE, id);
		return WS.url(url).execute().map(x -> ok(x.asJson()));
	}
	
	private static SearchResponse query(String field, String term, double[] coordinates) {
			GeoPolygonFilterBuilder geoFilter =
					FilterBuilders.geoPolygonFilter("location").addPoint(coordinates[0], coordinates[1])
							.addPoint(coordinates[2], coordinates[3]).addPoint(coordinates[4], coordinates[5])
							.addPoint(coordinates[6], coordinates[7]);
			FilteredQueryBuilder geoQuery =
					QueryBuilders.filteredQuery(QueryBuilders.matchQuery(field, term), geoFilter);
			SearchResponse responseOfSearch =
					client.prepareSearch("organisations").setTypes("dbs")
							.setSearchType(SearchType.DFS_QUERY_AND_FETCH).setQuery(geoQuery)
							.execute().actionGet();
			return responseOfSearch;		
	}
}
