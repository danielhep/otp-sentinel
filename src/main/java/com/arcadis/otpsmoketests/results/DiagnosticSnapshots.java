package com.arcadis.otpsmoketests.results;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.assertions.*;
import org.opentripplanner.client.model.*;
import org.opentripplanner.client.parameters.InputBanned;
import org.opentripplanner.client.parameters.InputTriangle;
import org.opentripplanner.client.parameters.TripPlanParameters;

/** Converts library evaluations and resolved requests into diagnostic report snapshots. */
public final class DiagnosticSnapshots {

  private DiagnosticSnapshots() {}

  public static Snapshots.Parameters parameters(TripPlanParameters p) {
    return new Snapshots.Parameters(
      p.fromPlace().toPlaceString(),
      p.toPlace().toPlaceString(),
      p.time().toString(),
      p.searchDirection().name(),
      p.numItineraries(),
      p.wheelchair(),
      p.modes().stream().map(Enum::name).sorted().toList(),
      p.searchWindow().map(java.time.Duration::toSeconds).orElse(null),
      p.walkReluctance().orElse(null),
      p.carReluctance().orElse(null),
      p.bikeReluctance().orElse(null),
      p.bikeWalkingReluctance().orElse(null),
      p.walkSpeed().orElse(null),
      p.optimize() == null ? null : p.optimize().name(),
      p.pageCursor().orElse(null),
      p.banned().map(DiagnosticSnapshots::banned).orElse(null),
      p.triangle().map(DiagnosticSnapshots::triangle).orElse(null)
    );
  }

  private static Snapshots.Banned banned(InputBanned input) {
    var banned = input.toGenerated();
    return new Snapshots.Banned(
      banned.getRoutes(),
      banned.getAgencies(),
      banned.getTrips(),
      banned.getStops(),
      banned.getStopsHard()
    );
  }

  private static Snapshots.Triangle triangle(InputTriangle input) {
    var triangle = input.toGenegerated();
    return new Snapshots.Triangle(
      triangle.getSafetyFactor(),
      triangle.getSlopeFactor(),
      triangle.getTimeFactor()
    );
  }

  public static Snapshots.Expectation expectation(PlanMatchResult r) {
    return new Snapshots.Expectation(
      r.description(),
      r.matches(),
      r.matchingMode() == null ? null : r.matchingMode().name(),
      r.expectedLegs(),
      r.itineraryCriteria(),
      plan(r.plan()),
      map(r.children(), DiagnosticSnapshots::expectation),
      map(r.itineraryResults(), DiagnosticSnapshots::itineraryResult)
    );
  }

  public static Snapshots.Plan plan(TripPlan plan) {
    return new Snapshots.Plan(
      map(plan.itineraries(), DiagnosticSnapshots::itinerary),
      plan.nextPageCursor(),
      plan.previousPageCursor()
    );
  }

  private static Snapshots.Itinerary itinerary(Itinerary itinerary) {
    var score = itinerary.accessibilityScore();
    return new Snapshots.Itinerary(
      map(itinerary.legs(), DiagnosticSnapshots::leg),
      score.isPresent() ? score.getAsDouble() : null
    );
  }

  private static Snapshots.ItineraryResult itineraryResult(
    ItineraryMatchResult r
  ) {
    return new Snapshots.ItineraryResult(
      r.itineraryIndex(),
      r.isSuccess(),
      r.errors(),
      map(r.extraMatches(), DiagnosticSnapshots::leg),
      map(r.legResults(), DiagnosticSnapshots::requirement)
    );
  }

  private static Snapshots.Requirement requirement(LegRequirementResult r) {
    return new Snapshots.Requirement(
      r.expectedLegIndex(),
      r.status().name(),
      r.expectedCriteria(),
      r.matchedLeg().map(DiagnosticSnapshots::candidate).orElse(null),
      map(r.candidates(), DiagnosticSnapshots::candidate)
    );
  }

  private static Snapshots.Candidate candidate(LegMatchResult r) {
    return new Snapshots.Candidate(
      r.actualLegIndex(),
      leg(r.leg()),
      r.matchingCriteria(),
      r.missingCriteria()
    );
  }

  private static Snapshots.Leg leg(Leg leg) {
    return new Snapshots.Leg(
      text(leg.mode()),
      text(leg.startTime()),
      text(leg.endTime()),
      leg.duration() == null ? null : leg.duration().toSeconds(),
      leg.distance(),
      leg.interlineWithPreviousLeg(),
      leg.realTime(),
      place(leg.from()),
      place(leg.to()),
      leg.headsign().orElse(null),
      leg.route() == null ? null : route(leg.route()),
      leg.geometry() == null ? null : leg.geometry().toGoogleEncoding()
    );
  }

  private static Snapshots.Route route(
    org.opentripplanner.api.types.Route route
  ) {
    return new Snapshots.Route(
      route.getGtfsId(),
      route.getShortName(),
      route.getLongName()
    );
  }

  private static Snapshots.Place place(Place place) {
    return place == null
      ? null
      : new Snapshots.Place(place.name(), place.lat(), place.lon());
  }

  private static String text(Object value) {
    return value == null ? null : value.toString();
  }

  private static <T, R> List<R> map(
    Collection<T> values,
    Function<T, R> convert
  ) {
    return values == null ? null : values.stream().map(convert).toList();
  }
}
