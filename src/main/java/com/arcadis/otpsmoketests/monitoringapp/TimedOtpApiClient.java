package com.arcadis.otpsmoketests.monitoringapp;

import com.arcadis.otpsmoketests.results.DiagnosticSnapshots;
import com.arcadis.otpsmoketests.results.Results;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.client.OtpApiClient;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.parameters.TripPlanParameters;

public class TimedOtpApiClient extends OtpApiClient {

  private final MeterRegistry meterRegistry;
  private final String suiteName;
  private final String endpoint;
  private final ZoneId zoneId;
  private final List<Results.Request> requests = new ArrayList<>();

  public TimedOtpApiClient(
    ZoneId zoneId,
    String webUrl,
    MeterRegistry meterRegistry,
    String suiteName
  ) {
    super(zoneId, webUrl);
    this.meterRegistry = meterRegistry;
    this.suiteName = suiteName;
    this.endpoint = webUrl;
    this.zoneId = zoneId;
  }

  @Override
  public TripPlan plan(TripPlanParameters parameters) throws IOException {
    var snapshot = DiagnosticSnapshots.parameters(parameters);
    TripPlan response = null;
    try {
      response = super.plan(parameters);
      return response;
    } finally {
      JsonNode responseSnapshot = null;
      if (response != null) {
        try {
          responseSnapshot = DiagnosticSnapshots.plan(response);
        } catch (RuntimeException ignored) {
          // Diagnostics must not change the OTP request outcome.
        }
      }
      requests.add(
        new Results.Request(
          endpoint,
          zoneId.getId(),
          snapshot,
          responseSnapshot
        )
      );
    }
  }

  /** Called by the runner after each test, including tests that throw. */
  public List<Results.Request> drainRequests() {
    var captured = List.copyOf(requests);
    requests.clear();
    return captured;
  }

  public TripPlan timedPlan(TripPlanParameters parameters, String testName)
    throws IOException {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      return plan(parameters);
    } finally {
      Timer timer = meterRegistry.timer(
        MetricNames.planRequestTimer(suiteName, testName)
      );
      sample.stop(timer);
    }
  }
}
