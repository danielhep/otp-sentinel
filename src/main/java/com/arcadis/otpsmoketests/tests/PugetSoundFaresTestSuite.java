package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("puget-sound-fares")
@DisplayName("Puget Sound Fares")
public class PugetSoundFaresTestSuite extends PugetSoundTestSuite {

  public PugetSoundFaresTestSuite(String baseUrl, String deploymentName) {
    super("PugetSoundFares", baseUrl, deploymentName);
  }

  @Test
  @DisplayName("E Line ORCA fares")
  public void testELineBusFares() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("OLIVE_WAY"))
      .withTo(location("SHORELINE"))
      .build();

    var plan = apiClient.timedPlan(params, "testELineBusFares");

    assertPlan(
      plan,
      containingTransitLegs(
        route("E Line")
          .withMode("BUS")
          .withFarePrice(3.00f, "orca:regular", "orca:cash")
          .withFarePrice(1.0f, "orca:senior", "orca:cash")
          .withFarePrice(1.0f, "orca:special", "orca:electronic")
      )
    );
  }

  @Test
  @DisplayName("1 Line ORCA fares")
  public void testLightRailFares() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("RONALD_BOG_PARK"))
      .withTo(location("OLIVE_WAY"))
      .build();

    var plan = apiClient.timedPlan(params, "testLightRailFares");

    assertPlan(
      plan,
      containingTransitLegs(
        route("1 Line")
          .withMode("TRAM")
          .withFarePrice(3.0f, "orca:regular", "orca:cash")
          .withFarePrice(1.0f, "orca:senior", "orca:cash")
          .withFarePrice(1.0f, "orca:special", "orca:electronic")
      )
    );
  }

  @Test
  @DisplayName("Everett and Community Transit ORCA fares")
  public void testMultiLegJourneyFares() throws IOException {
    var params = defaultBuilder()
      .withFrom(location("CASINO_RD"))
      .withTo(location("MARYSVILLE"))
      .build();

    var plan = apiClient.timedPlan(params, "testMultiLegJourneyFares");

    assertPlan(
      plan,
      containingTransitLegs(
        route("8")
          .withMode("BUS")
          .withFarePrice(2.00f, "orca:regular", "orca:cash")
          .withFarePrice(2.00f, "orca:regular", "orca:electronic")
          .withFarePrice(1.00f, "orca:special", "orca:electronic"),
        route("201", "202")
          .withMode("BUS")
          .withFarePrice(2.50f, "orca:regular", "orca:cash")
          .withFarePrice(2.50f, "orca:regular", "orca:electronic")
          .withFarePrice(1.00f, "orca:special", "orca:electronic")
      )
    );
  }
}
