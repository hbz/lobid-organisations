@* Copyright 2016 Fabian Steeg, hbz. Licensed under the GPLv2 *@

@(json: String)

@import play.api.libs.json._
@import com.typesafe.config._

@string(value: JsValue) = { @value.asOpt[String].getOrElse("--") }

@defining(Json.parse(json)) { parsedContent =>

  @for((location, i) <- (parsedContent \ "location").asOpt[Seq[JsValue]].getOrElse(Seq()).zipWithIndex) {

    @defining(((parsedContent \ "name"),
           (parsedContent \ "classification" \ "id").as[String],
           Application.CONFIG.getObject("organisation.icons"))) { case(name, classification, icons)  =>

      locationDetails = "<table class='table table-striped table-condensed'>"
        + "<tr><td>Straße</td><td>@string((location \ "address" \ "streetAddress"))</td></tr>"
        + "<tr><td>Postleitzahl</td><td>@string((location \ "address" \ "postalCode"))</td></tr>"
        + "<tr><td>Stadt</td><td>@string((location \ "address" \ "addressLocality"))</td></tr>"
        + "<tr><td>Land</td><td>@string((location \ "address" \ "addressCountry"))</td></tr>"
        + "<tr><td>Öffnungzeiten</td><td>@string((location \ "openingHoursSpecification" \ "description"))</td></tr>"
        + "</table>";

      @if((location \ "geo" \ "lon").asOpt[String].isDefined && (location \ "geo" \ "lat").asOpt[String].isDefined) {
        var lat = @string((location \ "geo" \ "lat"))
        var lon = @string((location \ "geo" \ "lon"))
        var iconLabel = '@icons.getOrDefault(classification, ConfigValueFactory.fromAnyRef("library")).unwrapped()';
        var name = "@string(name)"
        makeMap(@i, lat, lon, iconLabel, name, locationDetails);
      } else {
	    document.getElementById("organisations-map@i").innerHTML = locationDetails;
      }
    }
  }
}

function makeMap(i, latCoord, lonCoord, iconLabel, name, locationDetails) {
  var layer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', {
    subdomains: '1234'
  });
  var center = new L.LatLng(latCoord, lonCoord)
  var map = new L.Map("organisations-map" + i, {
   center: center,
   zoom: 5,
   scrollWheelZoom: true,
   attributionControl: false,
   zoomControl: true
  });
  var latlng = L.latLng(latCoord, lonCoord);
  var icon = L.MakiMarkers.icon({icon: iconLabel, color: "#FF333B", size: "m"});
  var marker = L.marker([latCoord, lonCoord],{
    title: name,
    icon: icon
  });
  marker.addTo(map);
  zoomDetails(map, latlng, marker, locationDetails);
  marker.on('click', function(e) {
    zoomDetails(map, latlng, marker);
  });
  $('.nav-tabs a').on('shown.bs.tab', function(event){
    zoomDetails(map, latlng, marker, locationDetails);
  });
  map.addLayer(layer);
 }

function zoomDetails(map, latlng, marker, content) {
  map.invalidateSize(false);
  marker.bindPopup(content, {keepInView: true, minWidth: 300});
  map.setView(latlng, 16, {animate: false});
  marker.openPopup();
}