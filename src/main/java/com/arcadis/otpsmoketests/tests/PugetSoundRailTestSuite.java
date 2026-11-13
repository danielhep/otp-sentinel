package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.exactTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;

import java.io.IOException;
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
  @DisplayName("1 Line from Lynnwood City Center to Federal Way Downtown")
  public void lynnwoodCityCenterToFederalWayDowntown() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("lynnwood city center"))
      .withTo(location("federal way downtown"))
      .build();

    var plan = apiClient.timedPlan(
      params,
      "lynnwoodCityCenterToFederalWayDowntown"
    );

    assertPlan(plan, exactTransitLegs(route("1 Line").withMode("TRAM")));
  }

  @Test
  @DisplayName("2 Line from Lynnwood City Center to Downtown Redmond")
  public void lynnwoodCityCenterToDowntownRedmond() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("lynnwood city center"))
      .withTo(location("downtown redmond"))
      .build();

    var plan = apiClient.timedPlan(
      params,
      "lynnwoodCityCenterToDowntownRedmond"
    );

    assertPlan(plan, exactTransitLegs(route("2 Line").withMode("TRAM")));
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

    assertPlan(plan, exactTransitLegs(route("2 Line")));
  }
}
