package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.route;
import static org.opentripplanner.assertions.Expectations.routeLongName;

import java.io.IOException;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("puget-sound-local-on-demand")
@DisplayName("Puget Sound Local On-Demand")
public class PugetSoundLocalOnDemandTestSuite extends PugetSoundTestSuite {

  public PugetSoundLocalOnDemandTestSuite(
    String baseUrl,
    String deploymentName
  ) {
    super("PugetSoundLocalOnDemand", baseUrl, deploymentName);
  }

  @Test
  @DisplayName("Door-to-door service from Snoqualmie to North Bend")
  public void snoqualmieToNBend() throws IOException {
    var plan = flexPlanRequest("Snoqualmie", "NBend", "snoqualmieToNBend");
    checkLongName(plan, "Door-To-Door");
  }

  @Test
  @DisplayName("Zip Shuttle within Lynnwood")
  public void withinLynnwood() throws IOException {
    var plan = flexPlanRequest("Lynnwood", "Lynnwood2", "withinLynnwood");

    checkLongName(plan, "Zip Shuttle");
  }

  @Test
  @DisplayName("D'Arling Direct from Arlington to Darrington")
  public void arlingtonToDarrington() throws IOException {
    var plan = flexPlanRequest(
      "ArlingtonLib",
      "DarringtonLib",
      weekdayAtTime(LocalTime.of(7, 50)),
      "arlingtonToDarrington"
    );

    checkLongName(plan, "D'Arling Direct");
  }

  @Test
  @DisplayName("Test fixed-route connections from Arlington to Darrington")
  public void arlingtonToDarringtonFixedRoute() throws IOException {
    var params = defaultBuilder()
      .withNumberOfItineraries(20)
      .withFrom(location("ArlingtonLib"))
      .withTo(location("DarringtonLib"))
      .withTime(weekdayAtTime(LocalTime.of(7, 49)))
      .build();

    var plan = apiClient.timedPlan(params, "arlingtonToDarringtonFixedRoute");

    assertPlan(
      plan,
      containingTransitLegs(
        routeLongName("D'Arling Direct"),
        routeLongName("DC Direct")
      )
    );
  }

  @Test
  @DisplayName("Test Metro Flex service in North Seattle")
  public void metroFlexNorth() throws IOException {
    var plan = flexPlanRequest(
      "KenmorePR",
      "MountlakeTerraceTC",
      "metroFlexNorth"
    );

    checkLongName(plan, "Northshore");
  }

  @Test
  @DisplayName("Metro Flex in Tukwila")
  public void tukwilaFlex() throws IOException {
    var plan = flexPlanRequest("Burien", "TukwilaStn", "tukwilaFlex");

    checkLongName(plan, "Tukwila");
  }

  @Test
  @DisplayName("Ruston Runner service")
  public void rustonRunner() throws IOException {
    var plan = flexPlanRequest(
      "StadiumHS",
      "PtDefianceTerminal",
      "rustonRunner"
    );

    checkLongName(plan, "Runner");
  }

  @Test
  @DisplayName("1 Line to Zip")
  public void LinkToZip() throws IOException {
    var plan = flexPlanRequest("ShorelineNStation", "Lynnwood2", "1lineToZip");
    assertPlan(
      plan,
      containingTransitLegs(route("1 Line"), routeLongName("Zip Shuttle"))
    );
  }

  @Test
  @DisplayName("1 Line to Metro Flex Tukwila")
  public void LinkToMetroFlex() throws IOException {
    var plan = flexPlanRequest(
      "Seatac",
      "ThrondykeElementary",
      "LinkToMetroFlex"
    );

    assertPlan(
      plan,
      containingTransitLegs(route("1 Line"), routeLongName("Tukwila"))
    );
  }
}
