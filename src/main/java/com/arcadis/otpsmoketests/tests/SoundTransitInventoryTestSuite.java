package com.arcadis.otpsmoketests.tests;

import com.arcadis.otpsmoketests.BaseTestSuite;
import java.io.IOException;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Tag("smoke-test")
@Tag("soundtransit-inventory")
@DisplayName("Sound Transit Agency Inventory")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class SoundTransitInventoryTestSuite extends BaseTestSuite {

  public SoundTransitInventoryTestSuite(String baseUrl, String deploymentName) {
    super(
      deploymentName + ".SoundTransitInventory",
      baseUrl,
      "https://im5b1wfh6d.execute-api.us-east-1.amazonaws.com/commtrans/autocomplete",
      47.61097,
      -122.33701,
      ZoneId.of("America/Los_Angeles")
    );
  }

  @Override
  protected void initializeCoordinates() {}

  @Test
  @DisplayName("Verify all expected transit agencies are present")
  public void testAgencyList() throws IOException {
    var routes = this.apiClient.routes();
    var actualAgencies = routes
      .stream()
      .map(route -> route.getAgency().getName())
      .distinct()
      .sorted()
      .toList();

    var expectedAgencies = Stream
      .of(
        "City of Seattle",
        "Community Transit",
        "Everett Transit",
        "Intercity Transit",
        "Kitsap Transit",
        "Metro Transit",
        "Pierce Transit",
        "Seattle Center Monorail",
        "Skagit Transit",
        "Sound Transit",
        "Washington State Ferries",
        "Whatcom Transportation Authority"
      )
      .sorted()
      .toList();

    Assertions.assertTrue(
      actualAgencies.containsAll(expectedAgencies) &&
      expectedAgencies.containsAll(actualAgencies),
      String.format(
        "Agency lists differ.%nExpected: %s%nActual: %s%nMissing from expected: %s%nMissing from actual: %s",
        expectedAgencies,
        actualAgencies,
        expectedAgencies
          .stream()
          .filter(a -> !actualAgencies.contains(a))
          .toList(),
        actualAgencies
          .stream()
          .filter(a -> !expectedAgencies.contains(a))
          .toList()
      )
    );
  }
}
