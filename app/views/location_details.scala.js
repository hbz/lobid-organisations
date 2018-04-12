@* Copyright 2014-2016, hbz. Licensed under the Eclipse Public License 1.0 *@

@(json: String)

@import play.api.libs.json._
@import com.typesafe.config._
@import play.i18n._

@string(value: JsValue) = { @value.asOpt[String].getOrElse("--") }

@produceRow(name: String, value: JsValue) = {
  @value match {
    case JsString(valueAsString) if !valueAsString.isEmpty => {
      "<tr><td>@name</td><td>@valueAsString</td></tr>"
    }
    case _ => {""}
  }
}

@defining(Json.parse(json)) { parsedContent =>

  @for((location, i) <- (parsedContent \ "location").asOpt[Seq[JsValue]].getOrElse(Seq()).zipWithIndex) {

    @defining(((parsedContent \ "name"),
           (parsedContent \ "classification" \ "id").asOpt[String].getOrElse(""),
           Application.CONFIG.getObject("organisation.icons"))) { case(name, classification, icons)  =>

      locationDetails = "<table class='table table-striped table-condensed'>"
        + @produceRow(Messages.get("organisation.location.streetAddress"), location \ "address" \ "streetAddress")
        + @produceRow(Messages.get("organisation.location.postalCode"), location \ "address" \ "postalCode")
        + @produceRow(Messages.get("organisation.location.addressLocality"), location \ "address" \ "addressLocality")
        + @produceRow(Messages.get("organisation.location.addressCountry"), location \ "address" \ "addressCountry")
        + @produceRow(Messages.get("organisation.location.openingHoursSpecification"), location \ "address" \ "openingHoursSpecification")
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
  var layer = L.tileLayer('https://lobid.org/tiles/{z}/{x}/{y}.png', {
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
