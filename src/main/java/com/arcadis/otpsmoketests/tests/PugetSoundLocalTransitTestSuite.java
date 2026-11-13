package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.exactTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;
import static org.opentripplanner.assertions.PlanExpectation.itineraryCountBetween;

import java.io.IOException;
import java.time.LocalTime;
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

  @Test
  @DisplayName("NoahsTest")
  public void NoahsTest() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("35th and avalon"))
      .withTo(location("sea-tac airport terminal"))
      .withTime(weekdayAtTime(LocalTime.of(14, 40)))
      .build();

    var plan = apiClient.timedPlan(params, "NoahsTest");

    assertPlan(
      plan,
      exactTransitLegs(
        route("50").withMode("BUS"),
        route("1 Line").withMode("TRAM")
      )
    );
    assertPlan(
      plan,
      exactTransitLegs(
        route("21").withMode("BUS"),
        route("1 Line").withMode("TRAM")
      )
    );
  }

  @Test
  @DisplayName("King St Station to Tacoma Dome")
  public void testKingStStationToTacomaDome() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("king st station"))
      .withTo(location("tacoma dome"))
      .withTime(weekdayAtTime(LocalTime.of(17, 15)))
      .build();

    var plan = apiClient.timedPlan(params, "testKingStStationToTacomaDome");

    assertPlan(plan, exactTransitLegs(route("S Line")));
    assertPlan(plan, exactTransitLegs(route("590")));
    assertPlan(
      plan,
      exactTransitLegs(route("578", "577"), route("574", "586"))
    );
  }

  @Test
  @DisplayName("King St Station to 45th Veg Thai")
  public void testKingStStationTo45thVegThai() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("king st station"))
      .withTo(location("45th veg thai"))
      .build();

    var plan = apiClient.timedPlan(params, "testKingStStationTo45thVegThai");

    assertPlan(plan, exactTransitLegs(route("1 Line"), route("44")));

    assertPlan(plan, exactTransitLegs(route("62")));

    assertPlan(plan, exactTransitLegs(route("E Line")));
  }

  @Test
  @DisplayName("South Bellevue Station to Climate Pledge")
  public void testSouthBellevueStationToClimatePledge() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("s bellevue station"))
      .withTo(location("climate pledge"))
      .build();

    var plan = apiClient.timedPlan(
      params,
      "testSouthBellevueStationToClimatePledge"
    );

    assertPlan(plan, exactTransitLegs(route("2 Line"), route("D Line")));
    assertPlan(plan, exactTransitLegs(route("2 Line"), route("Monorail")));
  }

  @Test
  @DisplayName("SODO to N Capitol Hill")
  public void sodoToNCapitolHill() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("1st and lander"))
      .withTo(location("10th ave e and e howe st"))
      .withTime(weekdayAtTime(LocalTime.of(19, 45)))
      .build();

    var plan = apiClient.timedPlan(params, "sodoToNCapitolHill");

    assertPlan(plan, exactTransitLegs(route("1 Line")));

    assertPlan(plan, exactTransitLegs(route("1 Line"), route("49")));
  }

  @Test
  @DisplayName("King St Station to Mukilteo Ferry")
  public void kingStStationToMukilteoFerry() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("king st station"))
      .withTo(location("mukilteo ferry"))
      .withTime(weekdayAtTime(LocalTime.of(17, 35)))
      .build();

    var plan = apiClient.timedPlan(params, "kingStStationToMukilteoFerry");

    assertPlan(plan, exactTransitLegs(route("N Line")));

    assertPlan(
      plan,
      containingTransitLegs(route("1 Line", "2 Line"), route("117"))
    );
  }

  @Test
  @DisplayName("Green Lake to King St Station")
  public void greenLakeToKingStStation() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("green lake"))
      .withTo(location("king st station"))
      .build();

    var plan = apiClient.timedPlan(params, "greenLakeToKingStStation");

    assertPlan(plan, exactTransitLegs(route("1 Line")));

    assertPlan(plan, containingTransitLegs(route("62", "45"), route("1 Line")));
  }
}
