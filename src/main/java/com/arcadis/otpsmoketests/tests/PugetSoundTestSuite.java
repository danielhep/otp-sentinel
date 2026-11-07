package com.arcadis.otpsmoketests.tests;

import static org.opentripplanner.assertions.Expectations.containingTransitLegs;
import static org.opentripplanner.assertions.Expectations.routeLongName;
import static org.opentripplanner.client.model.RequestMode.FLEX_ACCESS;
import static org.opentripplanner.client.model.RequestMode.FLEX_DIRECT;
import static org.opentripplanner.client.model.RequestMode.FLEX_EGRESS;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import com.arcadis.otpsmoketests.BaseTestSuite;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.model.TripPlan;

/** Shared locations and request setup for independently selectable Puget Sound suites. */
@Tag("smoke-test")
@Tag("puget-sound")
@ExtendWith(TestParameterResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PugetSoundTestSuite extends BaseTestSuite {

  private record GeocodedLocation(String query, String source) {}

  private static final Map<String, GeocodedLocation> GEOCODED_LOCATIONS = Map.ofEntries(
    Map.entry(
      "colman dock",
      new GeocodedLocation("Colman Dock", "openstreetmap")
    ),
    Map.entry(
      "paul allen center",
      new GeocodedLocation("paul allen center", "openstreetmap")
    ),
    Map.entry(
      "king st station",
      new GeocodedLocation("King Street Station", "openstreetmap")
    ),
    Map.entry(
      "tacoma dome",
      new GeocodedLocation("Tacoma Dome", "openstreetmap")
    ),
    Map.entry(
      "45th veg thai",
      new GeocodedLocation("45th Vegetarian Thai", "openstreetmap")
    ),
    Map.entry(
      "s bellevue station",
      new GeocodedLocation("South Bellevue", "otp")
    ),
    Map.entry(
      "climate pledge",
      new GeocodedLocation("Climate Pledge", "openstreetmap")
    ),
    Map.entry(
      "1st and lander",
      new GeocodedLocation("1st ave s and s lander", "otp")
    ),
    Map.entry(
      "e prospect and broadway",
      new GeocodedLocation("10th Ave E & E Prospect St", "otp")
    ),
    Map.entry(
      "hopelink food bank",
      new GeocodedLocation("Hopelink Food Bank", "openstreetmap")
    ),
    Map.entry(
      "queen anne",
      new GeocodedLocation("1321 W Emerson St", "openaddresses")
    ),
    Map.entry(
      "tractor tavern ballard",
      new GeocodedLocation("Tractor Tavern", "openstreetmap")
    ),
    Map.entry(
      "mukilteo ferry",
      new GeocodedLocation("Mukilteo Ferry", "openstreetmap")
    ),
    Map.entry(
      "green lake",
      new GeocodedLocation("Little Red Hen", "openstreetmap")
    ),
    Map.entry("capitol hill", new GeocodedLocation("Capitol Hill", "otp")),
    Map.entry(
      "paseo fremont",
      new GeocodedLocation("N 43rd St & Fremont Ave N", "otp")
    ),
    Map.entry("columbia city", new GeocodedLocation("columbia city", "otp")),
    Map.entry(
      "1801 s bush pl",
      new GeocodedLocation("1801 S Bush Place", "openaddresses")
    ),
    Map.entry(
      "bellevue transit center",
      new GeocodedLocation("Bellevue Transit Center", "otp")
    ),
    Map.entry(
      "ShorelineNStation",
      new GeocodedLocation("Shoreline North/185th Station", "openstreetmap")
    ),
    Map.entry(
      "StadiumHS",
      new GeocodedLocation("Stadium High School", "openstreetmap")
    ),
    Map.entry(
      "PDZoo",
      new GeocodedLocation("Point Defiance Zoo", "openstreetmap")
    ),
    Map.entry(
      "SumnerSounder",
      new GeocodedLocation("Sumner Sounder Station", "openstreetmap")
    ),
    Map.entry(
      "SumnerSeniorCenter",
      new GeocodedLocation("Sumner Senior Center", "openstreetmap")
    ),
    Map.entry("Seatac", new GeocodedLocation("Seatac", "otp")),
    Map.entry(
      "ThrondykeElementary",
      new GeocodedLocation("Thorndyke Elementary", "openstreetmap")
    ),
    Map.entry(
      "MarysvilleAshPR",
      new GeocodedLocation(
        "Marysville Ash Ave Park & Ride (Community Transit)",
        "otp"
      )
    )
  );
  private static final Set<RequestMode> FLEX_MODES = Set.of(
    FLEX_DIRECT,
    FLEX_ACCESS,
    FLEX_EGRESS,
    TRANSIT,
    WALK
  );

  protected PugetSoundTestSuite(
    String suiteName,
    String baseUrl,
    String deploymentName
  ) {
    super(
      deploymentName + "." + suiteName,
      baseUrl,
      "https://im5b1wfh6d.execute-api.us-east-1.amazonaws.com/commtrans/autocomplete",
      47.61097,
      -122.33701,
      ZoneId.of("America/Los_Angeles")
    );
  }

  @Override
  protected void initializeCoordinates() {
    geocoder.add("SODO", 47.5811, -122.3290);
    geocoder.add("RONALD_BOG_PARK", 47.75601664, -122.33141);
    geocoder.add("SHORELINE", 47.7568, -122.3483);
    geocoder.add("OLIVE_WAY", 47.61309420, -122.336314916);
    geocoder.add("CASINO_RD", 47.92175434762228, -122.23896905562611);
    geocoder.add("MARYSVILLE", 48.05523331013222, -122.17763080699298);
    geocoder.add("Tacoma", 47.253304, -122.445237);
    geocoder.add("Puyallup", 47.189659, -122.295414);
    geocoder.add("Tukwila", 47.474005, -122.284023);
    geocoder.add("Bellevue", 47.620659, -122.187675);
    geocoder.add("Snoqualmie", 47.512752, -121.885709);
    geocoder.add("NBend", 47.492737, -121.788474);
    geocoder.add("Bellevue2", 47.571076, -122.163592);
    geocoder.add("Everett", 47.999847, -122.205627);
    geocoder.add("Lynnwood", 47.821271, -122.284036);
    geocoder.add("Lynnwood2", 47.847904, -122.272449);
    geocoder.add("Bothell", 47.780336, -122.211153);
    geocoder.add("Bothell2", 47.758795, -122.194675);
    geocoder.add("ArlingtonLib", 48.193364, -122.118405);
    geocoder.add("DarringtonLib", 48.2546724, -121.6037822);
    geocoder.add("KenmorePR", 47.759201, -122.243057);
    geocoder.add("MountlakeTerraceTC", 47.785057, -122.314788);
    geocoder.add("TukwilaStn", 47.4642067, -122.288452);
    geocoder.add("Burien", 47.474748, -122.283666);
    geocoder.add("FrontierMiddleSchool", 47.055521, -122.289541);
    geocoder.add("OrtingMiddleSchool", 47.101952, -122.217616);
    geocoder.add("PtDefianceTerminal", 47.305630422593595, -122.51442106465043);
  }

  /** Resolve only the locations a test uses, keeping unrelated geocoding out of suite setup. */
  protected Coordinate location(String key) throws IOException {
    var coordinate = geocoder.get(key);
    if (coordinate != null) return coordinate;
    var address = GEOCODED_LOCATIONS.get(key);
    if (address == null) throw new IllegalArgumentException(
      "Unknown Puget Sound location: " + key
    );
    geocoder.addGeocoded(key, address.query(), address.source());
    return geocoder.get(key);
  }

  protected TripPlan flexPlanRequest(
    String from,
    String to,
    LocalDateTime time,
    String testName
  ) throws IOException {
    var params = defaultBuilder()
      .withModes(FLEX_MODES)
      .withFrom(location(from))
      .withTo(location(to))
      .withTime(time)
      .build();
    return apiClient.timedPlan(params, testName);
  }

  protected TripPlan flexPlanRequest(String from, String to, String testName)
    throws IOException {
    return flexPlanRequest(from, to, weekdayAtNoon(), testName);
  }

  protected static void checkLongName(TripPlan plan, String longName) {
    assertPlan(plan, containingTransitLegs(routeLongName(longName)));
  }
}
