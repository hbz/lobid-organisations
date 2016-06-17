@* Copyright 2016 Fabian Steeg, hbz. Licensed under the GPLv2 *@

@(json: String)

@import play.api.libs.json._
@import com.typesafe.config._

@string(value: JsValue) = { @value.asOpt[String].getOrElse("--") }

@defining(Json.parse(json)) { parsedContent =>
@defining(((parsedContent \ "location").asOpt[Seq[JsValue]].getOrElse(Seq())(0), 
           (parsedContent \ "name"),
           (parsedContent \ "classification" \ "id").as[String],
           Application.CONFIG.getObject("organisation.icons"))) { case(location, name, classification, icons)  =>

var layer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', {
    subdomains: '1234'
});
var center = new L.LatLng(50, 10)
var map = new L.Map("organisations-map", {
    center: center,
    zoom: 5,
    scrollWheelZoom: true,
    attributionControl: false,
    zoomControl: true
});
@if((location \ "geo" \ "lon").asOpt[String].isDefined && (location \ "geo" \ "lat").asOpt[String].isDefined) {

    var lat = @string((location \ "geo" \ "lat"))
    var lon = @string((location \ "geo" \ "lon"))
    var latlng = L.latLng(lat, lon);
    var iconLabel = '@icons.getOrDefault(classification, ConfigValueFactory.fromAnyRef("library")).unwrapped()';
    var icon = L.MakiMarkers.icon({icon: iconLabel, color: "#FF333B", size: "m"});
    var marker = L.marker([lat, lon],{
        title: "@string(name)",
        icon: icon
    });

    locationDetails = "<table class='table table-striped table-condensed'>" 
        + "<tr><td>Straße</td><td>@string((location \ "address" \ "streetAddress"))</td></tr>"
        + "<tr><td>Postleitzahl</td><td>@string((location \ "address" \ "postalCode"))</td></tr>"
        + "<tr><td>Stadt</td><td>@string((location \ "address" \ "addressLocality"))</td></tr>"
        + "<tr><td>Land</td><td>@string((location \ "address" \ "addressCountry"))</td></tr>"
        + "<tr><td>Öffnungzeiten</td><td>@string((location \ "openingHoursSpecification" \ "description"))</td></tr>"
        + "</table>";
    bindPopup(locationDetails);
    marker.addTo(map);
    bindPopup(locationDetails);
    zoomDetails();
    marker.on('click', function(e) {
        zoomDetails();
    });
    
    function bindPopup(content) {
       marker.bindPopup(
           content,
       {
           keepInView: true,
           
       });
    }
    function zoomDetails() {
        map.setView(latlng, 16);
        marker.openPopup();
    }
    map.addLayer(layer);
}}}