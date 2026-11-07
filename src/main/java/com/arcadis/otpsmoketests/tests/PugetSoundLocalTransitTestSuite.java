package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.exactTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;
import static org.opentripplanner.assertions.PlanExpectation.itineraryCountBetween;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("puget-sound-local-transit")
@DisplayName("Puget Sound Local Transit")
public class PugetSoundLocalTransitTestSuite extends PugetSoundTestSuite {

  public PugetSoundLocalTransitTestSuite(
    String baseUrl,
    String deploymentName
  ) {
    super("PugetSoundLocalTransit", baseUrl, deploymentName);
  }

  @Test
  @DisplayName("Test E Line bus from Olive Way to Shoreline")
  public void testELineBus() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("OLIVE_WAY"))
      .withTo(location("SHORELINE"))
      .build();

    var plan = apiClient.timedPlan(params, "testELineBus");

    assertPlan(plan, containingTransitLegs(route("E Line").withMode("BUS")));
  }

  @Test
  @DisplayName("Test multi-leg journey from Casino Road to Marysville")
  public void testMultiLegJourney() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("CASINO_RD"))
      .withTo(location("MARYSVILLE"))
      .build();

    var plan = apiClient.timedPlan(params, "testMultiLegJourney");

    assertPlan(
      plan,
      containingTransitLegs(
        route("8").withMode("BUS"),
        route("201", "202").withMode("BUS")
      )
    );
  }

  @Test
  @DisplayName("SODO to King St Station")
  public void sodoToKingSt() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("1st and lander"))
      .withTo(location("king st station"))
      .build();

    var plan = apiClient.timedPlan(params, "sodoToKingSt");

    assertPlan(
      plan,
      exactTransitLegs(
        route("21"),
        route("5").withInterlinedWithPreviousLeg(true)
      )
    );
  }

  @Test
  @DisplayName("Request up to 10 itineraries from Olive Way to Shoreline")
  public void testCustomParameters() throws IOException {
    var customParams = defaultBuilder()
      .withFrom(location("OLIVE_WAY"))
      .withTo(location("SHORELINE"))
      .withNumberOfItineraries(10)
      .build();

    var plan = apiClient.timedPlan(customParams, "testCustomParameters");

    assertPlan(plan, itineraryCountBetween(1, 10));
  }

  @Test
  @DisplayName("Queen Anne to Hopelink Food Bank")
  public void queenAnneToHopelinkFoodBank() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("queen anne"))
      .withTo(location("hopelink food bank"))
      .build();

    var plan = apiClient.timedPlan(params, "queenAnneToHopelinkFoodBank");

    assertPlan(plan, containingTransitLegs(route("31", "32"), route("255")));
    assertPlan(plan, containingTransitLegs(route("31", "32"), route("239")));
  }

  @Test
  @DisplayName("King St Station to Tractor Tavern Ballard")
  public void kingStStationToTractorTavernBallard() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("king st station"))
      .withTo(location("tractor tavern ballard"))
      .build();

    var plan = apiClient.timedPlan(
      params,
      "kingStStationToTractorTavernBallard"
    );

    assertPlan(plan, exactTransitLegs(route("40")));
    assertPlan(plan, exactTransitLegs(route("D Line")));
  }

  @Test
  @DisplayName("Paseo Fremont to SODO")
  public void paseoFremontToSODO() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("paseo fremont"))
      .withTo(location("1st and lander"))
      .build();

    var plan = apiClient.timedPlan(params, "paseoFremontToSODO");

    assertPlan(
      plan,
      exactTransitLegs(
        route("5"),
        route("21").withInterlinedWithPreviousLeg(true)
      )
    );
  }
}
