package com.arcadis.otpsmoketests.results;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Versioned HTTP and storage types. No client models or exceptions cross this interface. */
public final class Results {

  private Results() {}

  public record Suite(
    String id,
    String deploymentName,
    String suiteName,
    String metricSuffix,
    String interval
  ) {}

  public record SuiteSummary(
    String id,
    String deploymentName,
    String suiteName,
    String metricSuffix,
    String interval,
    String status,
    String latestRunId,
    Instant completedAt
  ) {}

  public record Counts(long total, long passed, long failed) {}

  public record Request(
    String endpoint,
    String timeZone,
    JsonNode parameters,
    JsonNode response
  ) {}

  public record Failure(
    String kind,
    String exceptionType,
    String message,
    String stackTrace,
    JsonNode diagnostics
  ) {}

  public record Check(
    String testId,
    String testName,
    String status,
    long durationMs,
    List<Request> requests,
    Failure failure,
    String detailsMarkdown
  ) {
    public Check {
      requests = List.copyOf(requests);
    }
  }

  public record FailureRow(
    String suiteId,
    String runId,
    Instant completedAt,
    String testId,
    String testName,
    String kind,
    long durationMs,
    String summary,
    String detailsMarkdown
  ) {}

  public record Run(
    int schemaVersion,
    String suiteId,
    String deploymentName,
    String suiteName,
    String runId,
    String trigger,
    String status,
    Instant startedAt,
    Instant completedAt,
    long durationMs,
    Counts counts,
    List<Check> tests,
    List<FailureRow> failures
  ) {
    public Run {
      Objects.requireNonNull(runId);
      Objects.requireNonNull(suiteId);
      Objects.requireNonNull(startedAt);
      Objects.requireNonNull(completedAt);
      Objects.requireNonNull(counts);
      tests = List.copyOf(tests);
      failures = List.copyOf(failures);
    }
  }

  public record Latest(
    String suiteId,
    String deploymentName,
    String suiteName,
    String runId,
    String status,
    Instant startedAt,
    Instant completedAt,
    Long durationMs,
    Counts counts,
    List<FailureRow> failures
  ) {
    public static Latest of(Suite suite, Run run) {
      if (run == null) {
        return new Latest(
          suite.id(),
          suite.deploymentName(),
          suite.suiteName(),
          null,
          "never_run",
          null,
          null,
          null,
          null,
          List.of()
        );
      }
      return new Latest(
        suite.id(),
        run.deploymentName(),
        run.suiteName(),
        run.runId(),
        run.status(),
        run.startedAt(),
        run.completedAt(),
        run.durationMs(),
        run.counts(),
        run.failures()
      );
    }
  }
}
