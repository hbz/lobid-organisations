@* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 *@

@(queryMetadata: String, q: String, location: String, from: Int, size: Int)

@import play.api.libs.json._
@import com.typesafe.config._
var layer = L.tileLayer('https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}.png', {
	attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
});
var kassel = new L.LatLng(51.19, 9.30)
var map = new L.Map("facet-map", {
  center: kassel,
  zoom: 5,
  minZoom: 0,
  scrollWheelZoom: true,
  attributionControl: true,
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
  singleMarkerMode: false
});

var queryParams = '&q=@views.html.helper.urlEncode(q)';

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
    polygon[i] = result[i].lat.toFixed(6) + ',' + result[i].lng.toFixed(6);
  }
  var polygonQueryParam = polygon.join('+');
  console.log(polygonQueryParam);
  if(polygonQueryParam.length > 0) {
    location.href='/organisations/search?location=' + polygonQueryParam + queryParams;
  }
}

map.addLayer(layer);
window.onload = addMarkerLayer;

function addMarkerLayer(){
	var addedLocations = [];
	var latLngObjects = [];
	@for(bucket <- (Json.parse(queryMetadata) \ "aggregations" \ "location.geo" \ "buckets").as[Seq[JsObject]];
			Array(isil, latLon, name, classification, _*) = (bucket \ "key").as[String].split(";;;");
			if isil != "null" && latLon != "null" && name != "null" && classification != "null";
			freq = (bucket \ "doc_count").as[JsNumber]) {
	   var iconLabel = '@Application.CONFIG.getObject("organisation.icons").getOrDefault(classification, ConfigValueFactory.fromAnyRef("library")).unwrapped()';
	   addMarker('/organisations/@isil', '@latLon', '@freq', '@name', iconLabel);
	}
	map.addLayer(markers);

	$("#location-facet-loading").hide();
	if(addedLocations.length > 0){
		$("#location-facet").show();
		map.invalidateSize();
		map.fitBounds(latLngObjects, {reset: true});
	}

	function addMarker(link, latLon, freq, name, iconLabel){
		if(addedLocations.indexOf(latLon) == -1) {
		 var lat = latLon.split(",")[0];
		 var lon = latLon.split(",")[1];
		 var marker = L.marker([lat,lon],{
			 title : name,
			 icon : L.MakiMarkers.icon({icon: iconLabel, color: "#FF333B", size: "m"})
		 });
		 marker.on('click', function(e) {
		  location.href = link;
		 });
		 markers.addLayer(marker);
		 addedLocations.push(latLon);
		 latLngObjects.push(marker.getLatLng());
		}
	}
}
