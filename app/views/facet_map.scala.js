@* Copyright 2016 Fabian Steeg, hbz. Licensed under the GPLv2 *@

@(queryMetadata: String, q: String, location: String, from: Int, size: Int)

@import play.api.libs.json._

var layer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', { subdomains: '1234' });
var kassel = new L.LatLng(51.19, 9.30)
var map = new L.Map("facet-map", {
  center: kassel,
  zoom: 5,
  minZoom: 1,
  maxZoom: 15,
  scrollWheelZoom: true,
  attributionControl: false,
  zoomControl: true
});

var pointsParam = '@location';
if (pointsParam.length > 0) {
  var options = {color: "#FF333B"}
  var points = pointsParam.split(/[ +]/);
  var hull = [points.length];
  for (var i = 0; i < points.length; i++) {
    var latLon = points[i].split(",");
    hull[i] = L.latLng(latLon[0],latLon[1]);
  }
  var currentPolygon = new L.Polygon(hull, options);
  map.addLayer(currentPolygon);
  map.fitBounds(hull, {reset: true});
}

var markers = new L.MarkerClusterGroup({
  maxClusterRadius: 50,
  zoomToBoundsOnClick: false,
  singleMarkerMode: true
});

var queryParams = '&q=@views.html.helper.urlEncode(q)&format=html';

var lastLayer;
var label;
markers.on('clusterclick', function (a) {
  lastLayer = a.layer;
  areaSearch(lastLayer.getConvexHull());
});

function areaSearch(hull) {
  var points = hull.map(function(p) {
    return map.options.crs.latLngToPoint(p, map.getZoom());
  });
  var margined = new Offset().data(points).margin(0.1);
  var result = margined.map(function(p) {
    return map.options.crs.pointToLatLng(p, map.getZoom());
  });
  var polygon = [result.length];
  for ( var i = 0; i < result.length; i++ ) {
    polygon[i] = result[i].lat.toFixed(5) + ',' + result[i].lng.toFixed(5);
  }
  var polygonQueryParam = polygon.join('+');
  console.log(polygonQueryParam);
  if(polygonQueryParam.length > 0) {
    location.href='/organisations?location=' + polygonQueryParam + queryParams;
  }
}

map.addLayer(layer);
window.onload = addMarkerLayer;

function addMarkerLayer(){
	@for(bucket <- (Json.parse(queryMetadata) \ "aggregations" \ "location.geo" \ "buckets").as[Seq[JsObject]];
			latLon = (bucket \ "key").as[String];
			freq = (bucket \ "doc_count").as[JsNumber]) {
	   addMarker('/organisations?location='+'@latLon'+',1'+queryParams, '@latLon', '@freq');
	}
	map.addLayer(markers);

	function addMarker(link, latLon, freq){
	 var lat = latLon.split(",")[0];
	 var lon = latLon.split(",")[1];
	 var marker = L.marker([lat,lon],{});
	 marker.on('click', function(e) {
	  location.href = link;
	 });
	 markers.addLayer(marker);
	}
}
