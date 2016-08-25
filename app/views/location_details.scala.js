@* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 *@

@(json: String)

@import play.api.libs.json._
@import com.typesafe.config._
@import play.i18n._

@string(value: JsValue) = { @value.asOpt[String].getOrElse("--") }

@defining(Json.parse(json)) { parsedContent =>

  @for((location, i) <- (parsedContent \ "location").asOpt[Seq[JsValue]].getOrElse(Seq()).zipWithIndex) {

    @defining(((parsedContent \ "name"),
           (parsedContent \ "classification" \ "id").as[String],
           Application.CONFIG.getObject("organisation.icons"))) { case(name, classification, icons)  =>

      locationDetails = "<table class='table table-striped table-condensed'>"
        + "<tr><td>@Messages.get("organisation.location.streetAddress")</td><td>@string((location \ "address" \ "streetAddress"))</td></tr>"
        + "<tr><td>@Messages.get("organisation.location.postalCode")</td><td>@string((location \ "address" \ "postalCode"))</td></tr>"
        + "<tr><td>@Messages.get("organisation.location.addressLocality")</td><td>@string((location \ "address" \ "addressLocality"))</td></tr>"
        + "<tr><td>@Messages.get("organisation.location.addressCountry")</td><td>@string((location \ "address" \ "addressCountry"))</td></tr>"
        + "<tr><td>@Messages.get("organisation.location.openingHoursSpecification")</td><td>@string((location \ "openingHoursSpecification" \ "description"))</td></tr>"
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
  var layer = L.tileLayer('https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}.png', {
	attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
  });
  var center = new L.LatLng(latCoord, lonCoord)
  var map = new L.Map("organisations-map" + i, {
   center: center,
   zoom: 5,
   scrollWheelZoom: true,
   attributionControl: true,
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
    zoomDetails(map, latlng, marker, locationDetails);
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