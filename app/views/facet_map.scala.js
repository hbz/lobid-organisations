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
  singleMarkerMode: false
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
    location.href='/organisations/search?location=' + polygonQueryParam + queryParams;
  }
}

map.addLayer(layer);
window.onload = addMarkerLayer;

function addMarkerLayer(){
	@for(bucket <- (Json.parse(queryMetadata) \ "aggregations" \ "location.geo" \ "buckets").as[Seq[JsObject]];
			Array(isil, latLon, name, classification, _*) = (bucket \ "key").as[String].split(";;;");
			if isil != "null" && latLon != "null" && name != "null" && classification != "null";
			freq = (bucket \ "doc_count").as[JsNumber]) {
		addMarker('/organisations/@isil?format=html', '@latLon', '@freq', '@name', @classification.takeRight(2));
	}
	map.addLayer(markers);

	function addMarker(link, latLon, freq, name, code){
	 var lat = latLon.split(",")[0];
	 var lon = latLon.split(",")[1];
	 var icon = "library"
	if(code === 34) {
	    icon = "music"
	} else if (code === 39) {
	    icon = "bus"
	} else if (code === 60 || code === 65 || code === 73 || code === 81 || code === 84) {
	    icon = "college"
	} else if (code > 50 && code < 60) {
	    icon = "town-hall"
	} else if (code === 82) {
	    icon = "monument"
	} else if (code === 86) {
	    icon = "museum"
	}
	 var marker = L.marker([lat,lon],{
		 title : name,
		 icon : L.MakiMarkers.icon({icon: icon, color: "#FF333B", size: "m"})
	 });
	 marker.on('click', function(e) {
	  location.href = link;
	 });
	 markers.addLayer(marker);
	}
}
