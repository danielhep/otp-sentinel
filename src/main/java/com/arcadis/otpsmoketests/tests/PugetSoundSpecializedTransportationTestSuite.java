package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.routeLongName;

import java.io.IOException;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("puget-sound-specialized-transportation")
@DisplayName("Puget Sound Specialized Transportation")
public class PugetSoundSpecializedTransportationTestSuite
  extends PugetSoundTestSuite {

  public PugetSoundSpecializedTransportationTestSuite(
    String baseUrl,
    String deploymentName
  ) {
    super("PugetSoundSpecializedTransportation", baseUrl, deploymentName);
  }

  @Test
  @DisplayName("Specialized transportation: Tacoma to Puyallup")
  public void insideTacoma() throws IOException {
    var plan = flexPlanRequest("Tacoma", "Puyallup", "insideTacoma");

    assertPlan(plan, containingTransitLegs(routeLongName("Road to Recovery")));
    assertPlan(
      plan,
      containingTransitLegs(routeLongName("Volunteer Services: Southwest"))
    );
  }

  @Test
  @DisplayName("Specialized transportation: Tukwila to Bellevue")
  public void burienToBellevue() throws IOException {
    var plan = flexPlanRequest("Tukwila", "Bellevue", "burienToBellevue");

    assertPlan(
      plan,
      containingTransitLegs(routeLongName("Volunteer Transportation"))
    );
    assertPlan(plan, containingTransitLegs(routeLongName("Road to Recovery")));
    assertPlan(
      plan,
      containingTransitLegs(routeLongName("Medicaid Transportation"))
    );
  }

  @Test
  @DisplayName("Specialized transportation: Snoqualmie to North Bend")
  public void snoqualmieToNBend() throws IOException {
    var plan = flexPlanRequest("Snoqualmie", "NBend", "snoqualmieToNBend");
    checkLongName(plan, "Volunteer Transportation");
    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Volunteer Services: King County");
    checkLongName(plan, "Medicaid Transportation");
  }

  @Test
  @DisplayName("Specialized transportation: within Bellevue")
  public void insideBellevue() throws IOException {
    var plan = flexPlanRequest("Bellevue", "Bellevue2", "insideBellevue");

    checkLongName(plan, "Volunteer Transportation");
    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Hyde Shuttle");
    checkLongName(plan, "Volunteer Services: King County");
    checkLongName(plan, "Medicaid Transportation");
  }

  @Test
  @DisplayName("Specialized transportation: Marysville to Everett")
  public void marysvilleToEverett() throws IOException {
    var plan = flexPlanRequest(
      "MarysvilleAshPR",
      "Everett",
      "marysvilleToEverett"
    );

    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Medicaid Transportation");
    checkLongName(plan, "Paratransit");
  }

  @Test
  @DisplayName("Specialized transportation: within Lynnwood")
  public void withinLynnwood() throws IOException {
    var plan = flexPlanRequest("Lynnwood", "Lynnwood2", "withinLynnwood");

    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Medicaid Transportation");
  }

  @Test
  @DisplayName("Specialized transportation: within Bothell")
  public void withinBothell() throws IOException {
    var plan = flexPlanRequest("Bothell2", "Bothell", "withinBothell");

    checkLongName(plan, "Medicaid Transportation");
    checkLongName(plan, "Transportation Service");
    checkLongName(plan, "Volunteer Services: King County");
    checkLongName(plan, "Volunteer Transportation");
    checkLongName(plan, "Road to Recovery");
  }

  @Test
  @DisplayName("Specialized transportation: Arlington to Darrington")
  public void arlingtonToDarrington() throws IOException {
    var plan = flexPlanRequest(
      "ArlingtonLib",
      "DarringtonLib",
      weekdayAtTime(LocalTime.of(7, 50)),
      "arlingtonToDarrington"
    );

    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Medicaid Transportation");
  }

  @Test
  @DisplayName("Specialized transportation: within Tukwila")
  public void tukwilaFlex() throws IOException {
    var plan = flexPlanRequest("Burien", "TukwilaStn", "tukwilaFlex");

    checkLongName(plan, "Hyde Shuttle");
    checkLongName(plan, "Medicaid Transportation");
  }

  @Test
  @DisplayName("Specialized transportation: Graham to Orting")
  public void grahamToOrting() throws IOException {
    var plan = flexPlanRequest(
      "FrontierMiddleSchool",
      "OrtingMiddleSchool",
      "grahamToOrting"
    );

    checkLongName(plan, "Beyond the Borders");
    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Volunteer Services: Southwest");
  }

  @Test
  @DisplayName(
    "Specialized transportation: Stadium High School to Point Defiance Zoo"
  )
  public void StadiumHSToPDZoo() throws IOException {
    var plan = flexPlanRequest("StadiumHS", "PDZoo", "StadiumHSToPDZoo");

    checkLongName(plan, "Volunteer Services: Southwest");
    checkLongName(plan, "Road to Recovery");
  }

  @Test
  @DisplayName("Specialized transportation: Sumner Station to Senior Center")
  public void SumnerSounderToSeniorCenter() throws IOException {
    var plan = flexPlanRequest(
      "SumnerSounder",
      "SumnerSeniorCenter",
      weekdayAtTime(LocalTime.of(14, 40)),
      "SumnerSounderToSeniorCenter"
    );
    checkLongName(plan, "Beyond the Borders");
    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Volunteer Services: Southwest");
    checkLongName(plan, "Sumner-Bonney Lake Connector");
  }

  @Test
  @DisplayName(
    "Specialized transportation: Stadium High School to Point Defiance Terminal"
  )
  public void rustonRunner() throws IOException {
    var plan = flexPlanRequest(
      "StadiumHS",
      "PtDefianceTerminal",
      "rustonRunner"
    );

    checkLongName(plan, "Road to Recovery");
    checkLongName(plan, "Volunteer Services: Southwest");
  }
}
