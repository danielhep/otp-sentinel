# Puget Sound Java test suites

Puget Sound journeys are grouped by the data and behavior they require. Each
suite is an ordinary Java `@Test` suite with its own schedule, metrics, and
results. Enable the same suites against any deployment that contains the
required regional feeds. Agency ownership alone does not determine which
journeys a deployment can support.

| KDL class | Tests | Coverage |
| --- | ---: | --- |
| `PugetSoundLocalTransit` | 7 | E Line, Everett/Community Transit transfers, Seattle bus alternatives, both directions of 5/21 interlining, and the custom ten-itinerary request |
| `PugetSoundRail` | 12 | 1 Line, 2 Line, Sounder N/S, bus-to-rail transfers, and Monorail connections |
| `PugetSoundLocalOnDemand` | 9 | Zip Shuttle, Metro Flex Northshore/Tukwila, Runner, Snoqualmie Valley Door-To-Door, D'Arling Direct and its scheduled DC Direct connection, and rail-to-flex transfers |
| `PugetSoundSpecializedTransportation` | 13 | Medicaid, volunteer transportation, Road to Recovery, Paratransit, Hyde Shuttle, Beyond the Borders and its Sumner-Bonney Lake Connector, and the existing Bothell Transportation Service check |
| `PugetSoundFares` | 3 | ORCA prices, rider categories, and fare media for E Line, 1 Line, and the Everett/Community Transit transfer |
| `SoundTransit` | 1 | Sound Transit's deployment-specific, exact agency inventory; no journey tests |

All routing suites run without requiring fare data. `PugetSoundFares` makes
separate requests and owns every fare assertion. Bus-to-rail journeys belong in
Rail; rail-to-flex journeys belong in Local On-Demand. The rail suite preserves
the existing bus alternatives checked alongside rail journeys.

Local On-Demand and Specialized Transportation do not require each other's
services. Where a journey originally checked both, it is now independently
reported in each suite with only that suite's expectations. For example, the
Lynnwood local on-demand check requires Zip Shuttle, while the specialized
check requires Road to Recovery and Medicaid Transportation.

The distinction follows service eligibility rather than vehicle type:
[Snoqualmie Valley Door-To-Door](https://svtbus.org/riding-the-bus/services-overview/)
serves the general public, while
[Hyde Shuttle](https://soundgenerations.org/our-programs/transportation/hyde-shuttle/)
and [Beyond the Borders](https://www.piercecountywa.gov/4856/Beyond-The-Borders)
have rider eligibility requirements. The Sumner-Bonney Lake Connector is part of
Beyond the Borders, so its check remains with Specialized Transportation.
These tests check that itineraries expose the services; they do not validate
individual riders' eligibility or booking workflows.

## Configuration

```kdl
deployment name="Puget Sound OTP" url="https://otp.example.com" {
  test-suite name="Local Transit" class="PugetSoundLocalTransit" interval="*/10 * * * *"
  test-suite name="Rail" class="PugetSoundRail" interval="*/10 * * * *"
  test-suite name="Local On-Demand" class="PugetSoundLocalOnDemand" interval="*/10 * * * *"
  test-suite name="Specialized Transportation" class="PugetSoundSpecializedTransportation" interval="*/10 * * * *"

  // Include only when fare data is available.
  test-suite name="Fares" class="PugetSoundFares" interval="*/10 * * * *"
}
```

Omit suites whose required data is unavailable. The on-demand suites retain the
existing `FLEX_DIRECT`, `FLEX_ACCESS`, `FLEX_EGRESS`, `TRANSIT`, and `WALK` modes.
The Arlington scheduled-connection test is independent of the flex test and
uses `TRANSIT` + `WALK` with twenty requested itineraries. It stays with Local
On-Demand so the ordinary local-transit suite does not acquire a dependency on
the D'Arling/DC Direct feeds. Both Arlington requests use the next Friday, at
07:49 for the scheduled connection and 07:50 for flex. Other existing service
times and route alternatives are retained.

`PugetSoundTestSuite` shares locations, the Seattle geocoder focus, the deployment
timezone, and flex request setup. Coordinate locations are available immediately;
geocoded locations are resolved only when a test uses them. An unrelated geocoder
lookup cannot fail suite construction. The global geocoding cache still avoids
repeated lookups across suites and deployments.

## Migrating existing configurations

- Replace a former `SoundTransit` journey suite with `PugetSoundLocalTransit`,
  `PugetSoundRail`, and, where supported, `PugetSoundFares`. Keep a separate
  `SoundTransit` entry if that deployment should enforce the original exact
  agency inventory.
- Replace `HopeLink` with `PugetSoundLocalOnDemand` and
  `PugetSoundSpecializedTransportation`. `HopeLinkTestSuite` has been removed.
- Suite names determine generated result IDs and metric names. Choose distinct
  names for the new suites and update dashboard selectors. Each suite can have
  an explicit, unique `id` for stable result lookup; do not reuse one old ID for
  multiple new suites.

Plan-request timer names include the deployment and the Java suite identifier,
for example
`otp.plan.requests.Example.PugetSoundLocalOnDemand.withinLynnwood.duration`.
This keeps timings separate when two suites check different services along the
same journey. Update selectors for the old plan-request timer names as well.

The JSON suite format and Minneapolis suite are unchanged.
