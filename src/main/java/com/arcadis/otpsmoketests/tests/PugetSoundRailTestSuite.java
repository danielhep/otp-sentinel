package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.exactTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;

import java.io.IOException;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("puget-sound-rail")
@DisplayName("Puget Sound Rail")
public class PugetSoundRailTestSuite extends PugetSoundTestSuite {

  public PugetSoundRailTestSuite(String baseUrl, String deploymentName) {
    super("PugetSoundRail", baseUrl, deploymentName);
  }

  @Test
  @DisplayName("Test 1 Line light rail from Ronald Bog Park to Olive Way")
  public void testLightRail() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("RONALD_BOG_PARK"))
      .withTo(location("OLIVE_WAY"))
      .build();

    var plan = apiClient.timedPlan(params, "testLightRail");

    assertPlan(plan, containingTransitLegs(route("1 Line").withMode("TRAM")));
  }

  @Test
  @DisplayName("Colman Dock to UW CS Building")
  public void testColmanDockToUWCSBuilding() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("colman dock"))
      .withTo(location("paul allen center"))
      .build();

    var plan = apiClient.timedPlan(params, "testColmanDockToUWCSBuilding");

    assertPlan(plan, exactTransitLegs(route("1 Line").withMode("TRAM")));
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

    assertPlan(plan, exactTransitLegs(route("E Line"), route("44")));
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
      .withTo(location("e prospect and broadway"))
      .withTime(weekdayAtTime(LocalTime.of(19, 45)))
      .build();

    var plan = apiClient.timedPlan(params, "sodoToNCapitolHill");

    assertPlan(plan, exactTransitLegs(route("1 Line")));

    assertPlan(plan, exactTransitLegs(route("1 Line"), route("49")));

    assertPlan(
      plan,
      exactTransitLegs(route("50"), route("1 Line"), route("49"))
    );
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

  @Test
  @DisplayName("Capitol Hill to King St Station")
  public void capitolHillToKingStStation() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("capitol hill"))
      .withTo(location("king st station"))
      .build();

    var plan = apiClient.timedPlan(params, "capitolHillToKingStStation");

    assertPlan(plan, exactTransitLegs(route("1 Line")));
  }

  @Test
  @DisplayName("Capitol Hill to SODO")
  public void capitolHillToUWStation() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("capitol hill"))
      .withTo(location("SODO"))
      .build();

    var plan = apiClient.timedPlan(params, "capitolHillToUWStation");

    assertPlan(plan, exactTransitLegs(route("1 Line")));
  }

  @Test
  @DisplayName("Capitol Hill to Columbia City")
  public void capitolHillToColumbiaCity() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("capitol hill"))
      .withTo(location("columbia city"))
      .build();

    var plan = apiClient.timedPlan(params, "capitolHillToColumbiaCity");

    assertPlan(plan, exactTransitLegs(route("1 Line")));
  }

  @Test
  @DisplayName("1801 S Bush Pl to Bellevue Transit Center")
  public void s1801SBushPlaceToBellevueTransitCenter() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("1801 s bush pl"))
      .withTo(location("bellevue transit center"))
      .build();

    var plan = apiClient.timedPlan(
      params,
      "s1801SBushPlaceToBellevueTransitCenter"
    );

    assertPlan(plan, exactTransitLegs(route("554"), route("2 Line")));
  }
}
