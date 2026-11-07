package com.arcadis.otpsmoketests.monitoringapp;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.assertions.Expectations.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.arcadis.otpsmoketests.BaseTestSuite;
import com.arcadis.otpsmoketests.ExecutableTestCase;
import com.arcadis.otpsmoketests.configuration.ConfigurationLoader;
import com.arcadis.otpsmoketests.results.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.assertions.PlanExpectation;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.slf4j.LoggerFactory;

class ResultsApiTest {

  @TempDir
  Path directory;

  private HttpServer otp;
  private Javalin app;
  private String otpUrl;
  private volatile String route = "999";
  private volatile int responseStatus = 200;
  private volatile String responseBody;
  private final AtomicInteger requests = new AtomicInteger();
  private final CountDownLatch requestEntered = new CountDownLatch(1);
  private final CountDownLatch releaseRequest = new CountDownLatch(1);
  private volatile boolean blockRequest;
  private volatile boolean omitEndTime;
  private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT
  );
  private final java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

  @BeforeEach
  void startOtp() throws Exception {
    otp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    otp.createContext(
      "/otp/gtfs/v1",
      exchange -> {
        requests.incrementAndGet();
        exchange.getRequestBody().readAllBytes();
        requestEntered.countDown();
        if (blockRequest) {
          try {
            if (
              !releaseRequest.await(10, TimeUnit.SECONDS)
            ) throw new AssertionError("Request was not released");
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new java.io.IOException(e);
          }
        }
        String body =
          """
        {"data":{"plan":{"itineraries":[{"legs":[{
          "from":{"name":"Downtown","lat":44.9,"lon":-93.2},
          "to":{"name":"Airport","lat":44.95,"lon":-93.25},
          "startTime":1793637000000,"endTime":1793637300000,
          "mode":"BUS","duration":300,"distance":400,"fareProducts":[],
          "route":{"gtfsId":"feed:r","shortName":"%s"},
          "legGeometry":{"points":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"}
        }]}]}}}
        """.formatted(
              route
            );
        if (omitEndTime) body = body.replace("\"endTime\":1793637300000,", "");
        if (responseBody != null) body = responseBody;
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        try (var output = exchange.getResponseBody()) {
          output.write(bytes);
        }
      }
    );
    otp.start();
    otpUrl = "http://127.0.0.1:" + otp.getAddress().getPort();
    BaseTestSuite.setMeterRegistry(registry);
  }

  @AfterEach
  void stop() {
    releaseRequest.countDown();
    if (app != null) app.stop();
    otp.stop(0);
    registry.close();
  }

  @Test
  void scheduledFailureManualRecoveryAndRestartKeepConsistentHistoricalReports()
    throws Exception {
    var runner = runner(JourneySuite.class, registry);
    serve(runner);
    assertEquals("never_run", get("/suites").get(0).path("status").asText());
    assertTrue(
      get("/suites/hopelink-prod.basic/latest").path("counts").isNull()
    );
    blockRequest = true;
    try (var executor = Executors.newSingleThreadExecutor()) {
      var execution = executor.submit(() ->
        runner.runSuite("hopelink-prod.basic", "scheduled")
      );
      assertTrue(requestEntered.await(10, TimeUnit.SECONDS));
      assertEquals(
        "never_run",
        get("/suites/hopelink-prod.basic/latest").path("status").asText()
      );
      releaseRequest.countDown();
      var run = execution.get(10, TimeUnit.SECONDS);
      assertTrue(Files.isRegularFile(directory.resolve(run.runId() + ".json")));
      var latest = get("/suites/hopelink-prod.basic/latest");
      assertEquals(run.runId(), latest.path("runId").asText());
      assertEquals(1, latest.at("/counts/failed").asInt());
      assertEquals(1, latest.at("/counts/passed").asInt());
      assertEquals(
        "Downtown to Airport",
        latest.at("/failures/0/testName").asText()
      );
      assertTrue(
        latest
          .at("/failures/0/detailsMarkdown")
          .asText()
          .contains("route '[560]'")
      );

      JsonNode historical = get("/runs/" + run.runId());
      assertEquals("scheduled", historical.path("trigger").asText());
      var journey = historical.at("/tests/1");
      assertEquals(
        "2026-11-06T12:00",
        journey.at("/requests/0/parameters/journeyTime").asText()
      );
      assertEquals(
        "America/Chicago",
        journey.at("/requests/0/timeZone").asText()
      );
      assertEquals(
        "999",
        journey
          .at("/requests/0/response/itineraries/0/legs/0/route/shortName")
          .asText()
      );
      assertEquals("assertion", journey.at("/failure/kind").asText());
      assertEquals(2, journey.at("/failure/diagnostics/children").size());
      assertTrue(
        journey.at("/failure/diagnostics/children/0/matches").asBoolean()
      );
      assertFalse(
        journey.at("/failure/diagnostics/children/1/matches").asBoolean()
      );
      assertTrue(
        journey
          .at(
            "/failure/diagnostics/children/1/children/0/itineraryResults/0/legResults/0/candidates/0/missingCriteria"
          )
          .toString()
          .contains("560")
      );
      assertTrue(
        registry
          .scrape()
          .contains("otp_tests_Hope_Link_Prod_Hope_Link_Basic_tests_failed 1.0")
      );

      route = "560";
      var manual = runner.runTestsManually();
      assertEquals(0L, manual.get("testsFailed"));
      var recovered = get("/suites/hopelink-prod.basic/latest");
      assertEquals("passed", recovered.path("status").asText());
      assertTrue(recovered.path("failures").isEmpty());
      assertNotEquals(run.runId(), recovered.path("runId").asText());
      assertEquals(historical, get("/runs/" + run.runId()));
      assertEquals(2, requests.get(), "Reading results must not run checks");

      app.stop();
      var restoredMetrics = new PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT
      );
      try {
        var restored = runner(JourneySuite.class, restoredMetrics);
        serve(restored);
        assertEquals(recovered, get("/suites/hopelink-prod.basic/latest"));
        assertEquals(historical, get("/runs/" + run.runId()));
        assertTrue(
          restoredMetrics
            .scrape()
            .contains(
              "otp_tests_Hope_Link_Prod_Hope_Link_Basic_tests_failed 0.0"
            )
        );
      } finally {
        restoredMetrics.close();
      }
    }
    assertEquals(404, request("/suites/unknown/latest").statusCode());
    assertEquals(404, request("/runs/unknown").statusCode());
  }

  @Test
  void publishingFailureStillUpdatesExecutionMetricsAndLogsReport()
    throws Exception {
    var runner = runner(JourneySuite.class, registry);
    Files.delete(directory);
    Files.createFile(directory);
    var logger = (Logger) LoggerFactory.getLogger(TestRunner.class);
    var logs = new ListAppender<ILoggingEvent>();
    logs.start();
    logger.addAppender(logs);
    long startedAt = Instant.now().getEpochSecond();
    try {
      assertThrows(
        IOException.class,
        () -> runner.runSuite("hopelink-prod.basic", "scheduled")
      );
      String prefix = "otp.tests.Hope Link Prod.Hope Link Basic.";
      assertAll(
        () -> assertEquals(
          2,
          registry.get(prefix + "tests_run").gauge().value()
        ),
        () -> assertEquals(
          1,
          registry.get(prefix + "tests_failed").gauge().value()
        ),
        () -> assertTrue(
          registry
            .get(prefix + "last_completed_timestamp_seconds")
            .gauge()
            .value() >= startedAt
        ),
        () -> assertTrue(
          logs.list
            .stream()
            .anyMatch(event ->
              event.getFormattedMessage().startsWith("Sentinel check failed:")
            )
        ),
        () -> assertTrue(
          logs.list
            .stream()
            .anyMatch(event ->
              event
                .getFormattedMessage()
                .startsWith("Sentinel suite completed:")
            )
        )
      );
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  @Test
  void requestFailureRetainsRequestWithoutInventingAssertionDiagnostics()
    throws Exception {
    responseStatus = 503;
    var runner = runner(JourneySuite.class, registry);
    serve(runner);
    var run = runner.runSuite("hopelink-prod.basic", "scheduled");
    var journey = get("/runs/" + run.runId()).at("/tests/1");
    assertEquals("request", journey.at("/failure/kind").asText());
    assertTrue(journey.at("/failure/diagnostics").isNull());
    assertTrue(journey.at("/requests/0/response").isNull());
    assertEquals(
      "2026-11-06T12:00",
      journey.at("/requests/0/parameters/journeyTime").asText()
    );
    assertEquals(
      200,
      request("/suites/hopelink-prod.basic/latest").statusCode()
    );
  }

  @Test
  void responseSnapshotFailurePreservesOtpResponseAndRequestRecord()
    throws Exception {
    responseBody = """
      {"data":{"plan":{"itineraries":[null]}}}
      """;
    var apiClient = new TimedOtpApiClient(
      ZoneId.of("America/Chicago"),
      otpUrl,
      registry,
      "diagnostics"
    );
    var parameters = TripPlanParameters
      .builder()
      .withFrom(new Coordinate(44.9, -93.2))
      .withTo(new Coordinate(44.95, -93.25))
      .withTime(LocalDateTime.of(2026, 11, 6, 12, 0))
      .withModes(Set.of(RequestMode.TRANSIT, RequestMode.WALK))
      .build();

    var response = apiClient.plan(parameters);

    assertNotNull(response);
    assertEquals(1, response.itineraries().size());
    assertNull(response.itineraries().getFirst());
    var captured = apiClient.drainRequests();
    assertEquals(1, captured.size());
    var request = captured.getFirst();
    assertEquals(otpUrl, request.endpoint());
    assertEquals("America/Chicago", request.timeZone());
    assertEquals(
      "44.9,-93.2::44.9,-93.2",
      request.parameters().fromPlace()
    );
    assertNull(request.response());
  }

  @Test
  void capturingIncompleteResponsesDoesNotTurnPassingChecksIntoFailures()
    throws Exception {
    route = "560";
    omitEndTime = true;
    var runner = runner(JourneySuite.class, registry);
    var run = runner.runSuite("hopelink-prod.basic", "scheduled");
    assertEquals("passed", run.status());
    assertNull(
      run
        .tests()
        .get(1)
        .requests()
        .getFirst()
        .response()
        .itineraries()
        .getFirst()
        .legs()
        .getFirst()
        .endTime()
    );
  }

  @ParameterizedTest
  @MethodSource("setupFailures")
  void suiteSetupFailureIsVisibleWithoutAnyJourneyRequests(
    Class<? extends BaseTestSuite> type,
    String message
  ) throws Exception {
    var runner = runner(type, registry);
    serve(runner);
    runner.runSuite("hopelink-prod.basic", "scheduled");
    var latest = get("/suites/hopelink-prod.basic/latest");
    assertEquals("error", latest.path("status").asText());
    assertEquals("suite_setup", latest.at("/failures/0/kind").asText());
    assertEquals(message, latest.at("/failures/0/summary").asText());
    assertEquals(0, requests.get());
  }

  static java.util.stream.Stream<Arguments> setupFailures() {
    return java.util.stream.Stream.of(
      Arguments.of(BrokenSuite.class, "Missing fixture configuration"),
      Arguments.of(
        EmptySuite.class,
        "Suite contained no checks. Check its test configuration."
      )
    );
  }

  private TestRunner runner(
    Class<? extends BaseTestSuite> type,
    PrometheusMeterRegistry metrics
  ) throws Exception {
    var config = ConfigurationLoader.loadFromString(
      """
      results retention-days=7 max-runs=100
      deployment name="Hope Link Prod" url="%s" {
        test-suite id="hopelink-prod.basic" name="Hope Link Basic" class="%s" interval="*/10 * * * *"
      }
      """.formatted(
          otpUrl,
          type.getName()
        )
    );
    return new TestRunner(metrics, config, directory, Clock.systemUTC());
  }

  private void serve(TestRunner runner) {
    app =
      Javalin.create(config -> {
        config.jsonMapper(new JavalinJackson(ResultJson.mapper(), false));
        config.jetty.defaultHost = "127.0.0.1";
      });
    ResultsApi.register(app, runner.results());
    app.start(0);
  }

  private HttpResponse<String> request(String path) throws Exception {
    return client.send(
      HttpRequest
        .newBuilder(
          URI.create("http://127.0.0.1:" + app.port() + "/api/v1" + path)
        )
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  private JsonNode get(String path) throws Exception {
    var response = request(path);
    assertEquals(200, response.statusCode(), response.body());
    assertEquals(
      "no-store",
      response.headers().firstValue("Cache-Control").orElseThrow()
    );
    return ResultJson.mapper().readTree(response.body());
  }

  public static class JourneySuite extends BaseTestSuite {

    public JourneySuite(String url, String deployment) {
      super(
        deployment,
        url,
        "http://127.0.0.1",
        44.9,
        -93.2,
        ZoneId.of("America/Chicago")
      );
    }

    @Override
    protected void initializeCoordinates() {}

    @Override
    public Collection<ExecutableTestCase> testCases() {
      return List.of(
        new ExecutableTestCase(
          "healthy",
          "Independent healthy check",
          () -> {}
        ),
        new ExecutableTestCase(
          "airport",
          "Downtown to Airport",
          () -> {
            var params = TripPlanParameters
              .builder()
              .withFrom(new Coordinate(44.9, -93.2))
              .withTo(new Coordinate(44.95, -93.25))
              .withTime(LocalDateTime.of(2026, 11, 6, 12, 0))
              .withModes(Set.of(RequestMode.TRANSIT, RequestMode.WALK))
              .build();
            assertPlan(
              apiClient.timedPlan(params, "airport"),
              PlanExpectation.allOf(
                PlanExpectation.itineraryCountBetween(1, 1),
                PlanExpectation.anyOf(
                  containingTransitLegs(route("560")),
                  containingTransitLegs(route("666"))
                )
              )
            );
          }
        )
      );
    }
  }

  public static final class BrokenSuite extends JourneySuite {

    public BrokenSuite(String url, String deployment) {
      super(url, deployment);
      throw new IllegalArgumentException("Missing fixture configuration");
    }
  }

  public static final class EmptySuite extends JourneySuite {

    public EmptySuite(String url, String deployment) {
      super(url, deployment);
    }

    @Override
    public Collection<ExecutableTestCase> testCases() {
      return List.of();
    }
  }
}
