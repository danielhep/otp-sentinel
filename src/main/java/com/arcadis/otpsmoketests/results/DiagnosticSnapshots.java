package com.arcadis.otpsmoketests.results;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.function.Function;
import org.opentripplanner.assertions.*;
import org.opentripplanner.client.model.*;
import org.opentripplanner.client.parameters.TripPlanParameters;

/** Explicit JSON projection of the library's evaluation tree and resolved requests. */
public final class DiagnosticSnapshots {

  private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

  private DiagnosticSnapshots() {}

  public static ObjectNode parameters(TripPlanParameters p) {
    var result = JSON.objectNode();
    result.put("fromPlace", p.fromPlace().toPlaceString());
    result.put("toPlace", p.toPlace().toPlaceString());
    result.put("journeyTime", p.time().toString());
    result.put("searchDirection", p.searchDirection().name());
    result.put("numItineraries", p.numItineraries());
    result.put("wheelchair", p.wheelchair());
    result.set(
      "modes",
      strings(p.modes().stream().map(Enum::name).sorted().toList())
    );
    p
      .searchWindow()
      .ifPresent(v -> result.put("searchWindowSeconds", v.toSeconds()));
    p.walkReluctance().ifPresent(v -> result.put("walkReluctance", v));
    p.carReluctance().ifPresent(v -> result.put("carReluctance", v));
    p.bikeReluctance().ifPresent(v -> result.put("bikeReluctance", v));
    p
      .bikeWalkingReluctance()
      .ifPresent(v -> result.put("bikeWalkingReluctance", v));
    p.walkSpeed().ifPresent(v -> result.put("walkSpeed", v));
    if (p.optimize() != null) result.put("optimize", p.optimize().name());
    p.pageCursor().ifPresent(v -> result.put("pageCursor", v));
    p
      .banned()
      .ifPresent(v -> {
        var banned = v.toGenerated();
        result.set(
          "banned",
          JSON
            .objectNode()
            .put("routes", banned.getRoutes())
            .put("agencies", banned.getAgencies())
            .put("trips", banned.getTrips())
            .put("stops", banned.getStops())
            .put("stopsHard", banned.getStopsHard())
        );
      });
    p
      .triangle()
      .ifPresent(v -> {
        var triangle = v.toGenegerated();
        result.set(
          "triangle",
          JSON
            .objectNode()
            .put("safetyFactor", triangle.getSafetyFactor())
            .put("slopeFactor", triangle.getSlopeFactor())
            .put("timeFactor", triangle.getTimeFactor())
        );
      });
    return result;
  }

  public static ObjectNode expectation(PlanMatchResult r) {
    var result = JSON
      .objectNode()
      .put("description", r.description())
      .put("matches", r.matches());
    result.put(
      "matchingMode",
      r.matchingMode() == null ? null : r.matchingMode().name()
    );
    result.set(
      "expectedLegs",
      array(r.expectedLegs(), DiagnosticSnapshots::strings)
    );
    result.set("itineraryCriteria", strings(r.itineraryCriteria()));
    result.set("plan", plan(r.plan()));
    result.set(
      "children",
      array(r.children(), DiagnosticSnapshots::expectation)
    );
    result.set(
      "itineraryResults",
      array(r.itineraryResults(), DiagnosticSnapshots::itineraryResult)
    );
    return result;
  }

  public static ObjectNode plan(TripPlan plan) {
    var result = JSON.objectNode();
    result.set(
      "itineraries",
      array(plan.itineraries(), DiagnosticSnapshots::itinerary)
    );
    result.put("nextPageCursor", plan.nextPageCursor());
    result.put("previousPageCursor", plan.previousPageCursor());
    return result;
  }

  private static ObjectNode itinerary(Itinerary itinerary) {
    var result = JSON.objectNode();
    result.set("legs", array(itinerary.legs(), DiagnosticSnapshots::leg));
    itinerary
      .accessibilityScore()
      .ifPresent(v -> result.put("accessibilityScore", v));
    return result;
  }

  private static ObjectNode itineraryResult(ItineraryMatchResult r) {
    var result = JSON
      .objectNode()
      .put("itineraryIndex", r.itineraryIndex())
      .put("matches", r.isSuccess());
    result.set("errors", strings(r.errors()));
    result.set(
      "extraMatches",
      array(r.extraMatches(), DiagnosticSnapshots::leg)
    );
    result.set(
      "legResults",
      array(r.legResults(), DiagnosticSnapshots::requirement)
    );
    return result;
  }

  private static ObjectNode requirement(LegRequirementResult r) {
    var result = JSON
      .objectNode()
      .put("expectedLegIndex", r.expectedLegIndex())
      .put("status", r.status().name());
    result.set("expectedCriteria", strings(r.expectedCriteria()));
    result.set(
      "matchedLeg",
      r.matchedLeg().map(DiagnosticSnapshots::candidate).orElse(null)
    );
    result.set(
      "candidates",
      array(r.candidates(), DiagnosticSnapshots::candidate)
    );
    return result;
  }

  private static ObjectNode candidate(LegMatchResult r) {
    var result = JSON.objectNode().put("actualLegIndex", r.actualLegIndex());
    result.set("leg", leg(r.leg()));
    result.set("matchingCriteria", strings(r.matchingCriteria()));
    result.set("missingCriteria", strings(r.missingCriteria()));
    return result;
  }

  private static ObjectNode leg(Leg leg) {
    var result = JSON
      .objectNode()
      .put("mode", text(leg.mode()))
      .put("startTime", text(leg.startTime()))
      .put("endTime", text(leg.endTime()))
      .put(
        "durationSeconds",
        leg.duration() == null ? null : leg.duration().toSeconds()
      )
      .put("distanceMeters", leg.distance())
      .put("interlineWithPreviousLeg", leg.interlineWithPreviousLeg())
      .put("realTime", leg.realTime());
    result.set("from", place(leg.from()));
    result.set("to", place(leg.to()));
    leg.headsign().ifPresent(v -> result.put("headsign", v));
    if (leg.route() != null) {
      result.set(
        "route",
        JSON
          .objectNode()
          .put("id", leg.route().getGtfsId())
          .put("shortName", leg.route().getShortName())
          .put("longName", leg.route().getLongName())
      );
    }
    if (leg.geometry() != null) result.put(
      "geometry",
      leg.geometry().toGoogleEncoding()
    );
    return result;
  }

  private static ObjectNode place(Place place) {
    if (place == null) return null;
    return JSON
      .objectNode()
      .put("name", place.name())
      .put("latitude", place.lat())
      .put("longitude", place.lon());
  }

  private static JsonNode strings(Collection<String> values) {
    return array(values, JSON::textNode);
  }

  private static String text(Object value) {
    return value == null ? null : value.toString();
  }

  private static <T> JsonNode array(
    Collection<T> values,
    Function<T, ? extends JsonNode> convert
  ) {
    var result = JSON.arrayNode();
    if (values == null) return JSON.nullNode();
    values.forEach(v -> result.add(convert.apply(v)));
    return result;
  }
}
