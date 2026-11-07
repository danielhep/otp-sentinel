package com.arcadis.otpsmoketests.results;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Diagnostic report types, independent of OTP client models. */
public final class Snapshots {

  private Snapshots() {}

  // Only optional fields are omitted when null; other nulls are part of the JSON format.
  public record Parameters(
    String fromPlace,
    String toPlace,
    String journeyTime,
    String searchDirection,
    int numItineraries,
    boolean wheelchair,
    List<String> modes,
    @JsonInclude(NON_NULL) Long searchWindowSeconds,
    @JsonInclude(NON_NULL) Double walkReluctance,
    @JsonInclude(NON_NULL) Double carReluctance,
    @JsonInclude(NON_NULL) Double bikeReluctance,
    @JsonInclude(NON_NULL) Double bikeWalkingReluctance,
    @JsonInclude(NON_NULL) Double walkSpeed,
    @JsonInclude(NON_NULL) String optimize,
    @JsonInclude(NON_NULL) String pageCursor,
    @JsonInclude(NON_NULL) Banned banned,
    @JsonInclude(NON_NULL) Triangle triangle
  ) {
    public Parameters {
      modes = copy(modes);
    }
  }

  public record Banned(
    String routes,
    String agencies,
    String trips,
    String stops,
    String stopsHard
  ) {}

  public record Triangle(
    Double safetyFactor,
    Double slopeFactor,
    Double timeFactor
  ) {}

  public record Expectation(
    String description,
    boolean matches,
    String matchingMode,
    List<List<String>> expectedLegs,
    List<String> itineraryCriteria,
    Plan plan,
    List<Expectation> children,
    List<ItineraryResult> itineraryResults
  ) {
    public Expectation {
      expectedLegs =
        expectedLegs == null
          ? null
          : expectedLegs.stream().map(Snapshots::copy).toList();
      itineraryCriteria = copy(itineraryCriteria);
      children = copy(children);
      itineraryResults = copy(itineraryResults);
    }
  }

  public record Plan(
    List<Itinerary> itineraries,
    String nextPageCursor,
    String previousPageCursor
  ) {
    public Plan {
      itineraries = copy(itineraries);
    }
  }

  public record Itinerary(
    List<Leg> legs,
    @JsonInclude(NON_NULL) Double accessibilityScore
  ) {
    public Itinerary {
      legs = copy(legs);
    }
  }

  public record ItineraryResult(
    int itineraryIndex,
    boolean matches,
    List<String> errors,
    List<Leg> extraMatches,
    List<Requirement> legResults
  ) {
    public ItineraryResult {
      errors = copy(errors);
      extraMatches = copy(extraMatches);
      legResults = copy(legResults);
    }
  }

  public record Requirement(
    int expectedLegIndex,
    String status,
    List<String> expectedCriteria,
    Candidate matchedLeg,
    List<Candidate> candidates
  ) {
    public Requirement {
      expectedCriteria = copy(expectedCriteria);
      candidates = copy(candidates);
    }
  }

  public record Candidate(
    int actualLegIndex,
    Leg leg,
    List<String> matchingCriteria,
    List<String> missingCriteria
  ) {
    public Candidate {
      matchingCriteria = copy(matchingCriteria);
      missingCriteria = copy(missingCriteria);
    }
  }

  public record Leg(
    String mode,
    String startTime,
    String endTime,
    Long durationSeconds,
    double distanceMeters,
    boolean interlineWithPreviousLeg,
    Boolean realTime,
    Place from,
    Place to,
    @JsonInclude(NON_NULL) String headsign,
    @JsonInclude(NON_NULL) Route route,
    @JsonInclude(NON_NULL) String geometry
  ) {}

  public record Route(String id, String shortName, String longName) {}

  public record Place(String name, float latitude, float longitude) {}

  private static <T> List<T> copy(List<T> values) {
    return values == null
      ? null
      : Collections.unmodifiableList(new ArrayList<>(values));
  }
}
