package com.arcadis.otpsmoketests.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.assertions.Expectations.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.api.types.OptimizeType;
import org.opentripplanner.assertions.PlanExpectation;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.parameters.*;
import org.opentripplanner.client.serialization.ObjectMappers;

class SnapshotJsonTest {

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void snapshotsKeepExistingJsonAndCanReadSavedReports(boolean populated)
    throws Exception {
    var mapper = ResultJson.mapper();
    String fixture = populated ? "populated.json" : "minimal.json";
    try (var input = getClass().getResourceAsStream("/snapshots/" + fixture)) {
      var expected = mapper.readTree(input);
      assertEquals(
        expected,
        mapper.readTree(mapper.writeValueAsString(snapshots(populated)))
      );
      var request = mapper.treeToValue(
        expected.get("request"),
        Results.Request.class
      );
      var failure = mapper.treeToValue(
        expected.get("failure"),
        Results.Failure.class
      );
      assertEquals(
        expected.get("request"),
        mapper.readTree(mapper.writeValueAsString(request))
      );
      assertEquals(
        expected.get("failure"),
        mapper.readTree(mapper.writeValueAsString(failure))
      );
    }
  }

  private static Map<String, Object> snapshots(boolean populated)
    throws Exception {
    var parameters = TripPlanParameters
      .builder()
      .withFrom(new Coordinate(44.9, -93.2))
      .withTo(new Coordinate(44.95, -93.25))
      .withTime(LocalDateTime.of(2026, 11, 6, 12, 0))
      .withModes(Set.of(RequestMode.WALK, RequestMode.TRANSIT));
    if (populated) {
      parameters
        .withSearchDirection(TripPlanParameters.SearchDirection.ARRIVE_BY)
        .withNumberOfItineraries(3)
        .withWheelchair(true)
        .withSearchWindow(Duration.ofSeconds(90))
        .withWalkReluctance(1.5)
        .withCarReluctance(2.5)
        .withBikeReluctance(3.5)
        .withBikeWalkingReluctance(4.5)
        .withWalkSpeed(1.25)
        .withOptimize(OptimizeType.TRIANGLE)
        .withPageCursor("next-page")
        .withBanned(
          new InputBanned(
            Set.of("feed:route"),
            Set.of(),
            Set.of("feed:trip"),
            Set.of(),
            Set.of("feed:stop")
          )
        )
        .withTriangle(new InputTriangle(0.2, 0.3, 0.5));
    }
    String response = populated
      ? """
        {"itineraries":[{"accessibilityScore":0.75,"legs":[{
          "from":{"name":"Downtown","lat":44.9,"lon":-93.2},
          "to":{"name":"Airport","lat":44.95,"lon":-93.25},
          "startTime":1793637000000,"endTime":1793637300000,
          "mode":"BUS","duration":300,"distance":400.5,
          "realTime":true,"interlineWithPreviousLeg":true,
          "headsign":"Airport","fareProducts":[],
          "route":{"gtfsId":"feed:r","shortName":"560"},
          "legGeometry":{"points":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"}
        }]}],"nextPageCursor":"next","previousPageCursor":"previous"}
        """
      : """
        {"itineraries":[{"legs":[{
          "from":{"name":null,"lat":0,"lon":0},
          "to":null,"fareProducts":[]
        }]}]}
        """;
    var plan = ObjectMappers
      .withTimezone(ZoneId.of("America/Chicago"))
      .readValue(response, TripPlan.class);
    var expectation = populated
      ? PlanExpectation.allOf(
          containingTransitLegs(route("560")),
          containingTransitLegs(route("999"))
        )
      : PlanExpectation.itineraryCountBetween(2, 3);
    return Map.of(
      "request",
      new Results.Request(
        "https://otp.example",
        "America/Chicago",
        DiagnosticSnapshots.parameters(parameters.build()),
        DiagnosticSnapshots.plan(plan)
      ),
      "failure",
      new Results.Failure(
        "assertion",
        "org.opentripplanner.assertions.ItineraryAssertionError",
        "Expected itinerary did not match",
        "stack trace",
        DiagnosticSnapshots.expectation(expectation.evaluate(plan))
      )
    );
  }
}
