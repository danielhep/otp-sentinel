package com.arcadis.otpsmoketests.monitoringapp;

import com.arcadis.otpsmoketests.BaseTestSuite;
import com.arcadis.otpsmoketests.configuration.Configuration;
import com.arcadis.otpsmoketests.results.*;
import com.arcadis.otpsmoketests.runner.CustomTestRunner;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import it.sauronsoftware.cron4j.Scheduler;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Owns execution, metrics and report publication for both scheduled and manual runs. */
class TestRunner {

  private static final Logger LOG = LoggerFactory.getLogger(TestRunner.class);
  private final Map<String, SuiteExecution> suites = new LinkedHashMap<>();
  private final ResultStore results;
  private final Clock clock;

  private record SuiteExecution(
    Results.Suite suite,
    Class<BaseTestSuite> clazz,
    String baseUrl,
    AtomicLong testsRun,
    AtomicLong testsFailed,
    AtomicLong durationMs,
    AtomicLong completedAt
  ) {}

  TestRunner(
    MeterRegistry registry,
    Configuration config,
    Path resultsDirectory,
    Clock clock
  ) throws IOException {
    this.clock = clock;
    for (var deployment : config.deploymentsUnderTest()) {
      for (var suite : deployment.suites()) addSuite(deployment, suite);
    }
    var settings = config.results();
    results =
      new ResultStore(
        resultsDirectory,
        suites.values().stream().map(SuiteExecution::suite).toList(),
        settings.retention(),
        settings.maxRuns(),
        clock
      );
    suites.values().forEach(execution -> registerMetrics(registry, execution));
  }

  ResultStore results() {
    return results;
  }

  private void addSuite(
    Configuration.DeploymentUnderTest deployment,
    Configuration.TestSuite config
  ) {
    String suffix =
      (deployment.name() + "_" + config.name()).replaceAll(
          "[^a-zA-Z0-9_:]",
          "_"
        );
    String id = config.id() == null
      ? UUID
        .nameUUIDFromBytes(
          (
            deployment.name().length() + ":" + deployment.name() + config.name()
          ).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        )
        .toString()
      : config.id();
    var suite = new Results.Suite(
      id,
      deployment.name(),
      config.name(),
      suffix,
      config.interval()
    );
    var execution = new SuiteExecution(
      suite,
      config.clazz(),
      deployment.url(),
      new AtomicLong(),
      new AtomicLong(),
      new AtomicLong(),
      new AtomicLong()
    );
    if (
      suites.putIfAbsent(id, execution) != null
    ) throw new IllegalArgumentException("Duplicate suite ID: " + id);
  }

  private void registerMetrics(
    MeterRegistry registry,
    SuiteExecution execution
  ) {
    var suite = execution.suite();
    var previous = results.latest(suite.id()).orElseThrow();
    if (previous.counts() != null) {
      execution.testsRun().set(previous.counts().total());
      execution.testsFailed().set(previous.counts().failed());
      execution.durationMs().set(previous.durationMs());
      execution.completedAt().set(previous.completedAt().getEpochSecond());
    }
    String prefix =
      "otp.tests." + suite.deploymentName() + "." + suite.suiteName() + ".";
    gauge(
      registry,
      prefix + "tests_run",
      execution.testsRun(),
      "Checks in the latest completed run"
    );
    gauge(
      registry,
      prefix + "tests_failed",
      execution.testsFailed(),
      "Failed checks in the latest completed run"
    );
    gauge(
      registry,
      prefix + "duration_ms",
      execution.durationMs(),
      "Latest completed run duration in milliseconds"
    );
    gauge(
      registry,
      prefix + "last_completed_timestamp_seconds",
      execution.completedAt(),
      "Latest completed run Unix timestamp"
    );
  }

  private static void gauge(
    MeterRegistry registry,
    String name,
    AtomicLong value,
    String description
  ) {
    Gauge
      .builder(name, value, AtomicLong::get)
      .description(description)
      .register(registry);
  }

  void scheduleTestSuites(Scheduler scheduler) {
    suites.forEach((id, execution) ->
      scheduler.schedule(execution.suite().interval(), () -> runScheduled(id))
    );
  }

  private void runScheduled(String suiteId) {
    try {
      runSuite(suiteId, "scheduled");
    } catch (IOException e) {
      LOG.error("Failed to persist scheduled results for suite {}", suiteId, e);
    }
  }

  Results.Run runSuite(String suiteId, String trigger) throws IOException {
    var execution = Objects.requireNonNull(
      suites.get(suiteId),
      "Unknown suite"
    );
    synchronized (execution) {
      return execute(execution, trigger);
    }
  }

  private Results.Run execute(SuiteExecution execution, String trigger)
    throws IOException {
    var suite = execution.suite();
    String runId = UUID.randomUUID().toString();
    var startedAt = clock.instant();
    try (
      var context = MDC.putCloseable("runId", runId);
      var suiteContext = MDC.putCloseable("suiteId", suite.id())
    ) {
      var outcome = CustomTestRunner.runTestSuite(
        execution.clazz(),
        suite.suiteName(),
        execution.baseUrl(),
        suite.deploymentName()
      );
      var report = RunReports.completed(
        suite,
        runId,
        trigger,
        startedAt,
        clock.instant(),
        outcome
      );
      results.publish(report);
      execution.testsRun().set(report.counts().total());
      execution.testsFailed().set(report.counts().failed());
      execution.durationMs().set(report.durationMs());
      execution.completedAt().set(report.completedAt().getEpochSecond());
      logReport(report);
      return report;
    }
  }

  private static void logReport(Results.Run report) {
    for (var failure : report.failures()) {
      LOG.error(
        "Sentinel check failed: {}",
        failure.testName(),
        StructuredArguments.kv("suiteId", report.suiteId()),
        StructuredArguments.kv("failure", failure)
      );
    }
    LOG.info(
      "Sentinel suite completed: {} ({})",
      report.suiteName(),
      report.status(),
      StructuredArguments.kv("suiteId", report.suiteId())
    );
  }

  Map<String, Object> runTestsManually() throws IOException {
    var start = clock.instant();
    List<Results.Run> reports = new ArrayList<>();
    for (var id : suites.keySet()) reports.add(runSuite(id, "manual"));
    Map<String, Object> stats = new LinkedHashMap<>();
    for (var report : reports) {
      stats.put(
        report.deploymentName() + "." + report.suiteName(),
        Map.of(
          "runId",
          report.runId(),
          "testsFound",
          report.counts().total(),
          "testsSucceeded",
          report.counts().passed(),
          "testsFailed",
          report.counts().failed(),
          "testsSkipped",
          0
        )
      );
    }
    var end = clock.instant();
    return Map.of(
      "runId",
      UUID.randomUUID().toString(),
      "startTime",
      start,
      "endTime",
      end,
      "durationMs",
      Duration.between(start, end).toMillis(),
      "testsFound",
      reports.stream().mapToLong(r -> r.counts().total()).sum(),
      "testsSucceeded",
      reports.stream().mapToLong(r -> r.counts().passed()).sum(),
      "testsFailed",
      reports.stream().mapToLong(r -> r.counts().failed()).sum(),
      "testsSkipped",
      0,
      "failures",
      reports
        .stream()
        .flatMap(r ->
          r
            .tests()
            .stream()
            .filter(t -> t.failure() != null)
            .map(t -> manualFailure(r, t))
        )
        .toList(),
      "suiteResults",
      stats
    );
  }

  private Map<String, Object> manualFailure(
    Results.Run report,
    Results.Check test
  ) {
    return Map.of(
      "deployment",
      report.deploymentName(),
      "suite",
      report.suiteName(),
      "testClass",
      suites.get(report.suiteId()).clazz().getSimpleName(),
      "testName",
      test.testId(),
      "errorMessage",
      test.failure().message(),
      "stackTrace",
      test.failure().stackTrace(),
      "runId",
      report.runId()
    );
  }
}
