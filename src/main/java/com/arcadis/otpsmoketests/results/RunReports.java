package com.arcadis.otpsmoketests.results;

import com.arcadis.otpsmoketests.runner.CustomTestRunner;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.opentripplanner.assertions.ItineraryAssertionError;

/** Converts completed executions into immutable, serializable report snapshots. */
public final class RunReports {

  private static final Pattern BACKTICKS = Pattern.compile("`+");

  private RunReports() {}

  public static Results.Run completed(
    Results.Suite suite,
    String runId,
    String trigger,
    Instant startedAt,
    Instant completedAt,
    CustomTestRunner.SuiteResult result
  ) {
    List<Results.Check> tests = result
      .testResults()
      .stream()
      .map(RunReports::check)
      .toList();
    List<Results.FailureRow> failures = tests
      .stream()
      .filter(t -> t.failure() != null)
      .map(t ->
        new Results.FailureRow(
          suite.id(),
          runId,
          completedAt,
          t.testId(),
          t.testName(),
          t.failure().kind(),
          t.durationMs(),
          summary(t.failure().message()),
          t.detailsMarkdown()
        )
      )
      .toList();
    if (tests.isEmpty()) {
      String message =
        "Suite contained no checks. Check its test configuration.";
      failures =
        List.of(
          new Results.FailureRow(
            suite.id(),
            runId,
            completedAt,
            null,
            "Suite discovery",
            "suite_setup",
            0,
            message,
            markdown(message)
          )
        );
    }
    String status = failures.isEmpty() ? "passed" : "failed";
    if (
      tests.isEmpty() ||
      failures.stream().anyMatch(f -> f.kind().equals("suite_setup"))
    ) status = "error";
    return new Results.Run(
      1,
      suite.id(),
      suite.deploymentName(),
      suite.suiteName(),
      runId,
      trigger,
      status,
      startedAt,
      completedAt,
      result.totalDurationMs(),
      new Results.Counts(
        result.getTestsFoundCount(),
        result.getTestsSucceededCount(),
        result.getTestsFailedCount()
      ),
      tests,
      failures
    );
  }

  private static Results.Check check(CustomTestRunner.TestResult test) {
    Results.Failure failure = failure(test);
    String details = failure == null ? "" : markdown(failure.message());
    return new Results.Check(
      test.testName(),
      test.displayName(),
      test.passed() ? "passed" : "failed",
      test.durationMs(),
      List.copyOf(test.requests()),
      failure,
      details
    );
  }

  private static Results.Failure failure(CustomTestRunner.TestResult test) {
    if (test.passed()) return null;
    Throwable error = test.exception();
    String kind = kind(test.testName(), error);
    if (error == null) return new Results.Failure(
      kind,
      null,
      "Unknown error",
      "",
      null
    );
    String message = error.getMessage() == null
      ? error.getClass().getSimpleName()
      : error.getMessage();
    var trace = new StringWriter();
    error.printStackTrace(new PrintWriter(trace));
    var diagnostics = error instanceof ItineraryAssertionError assertion
      ? DiagnosticSnapshots.expectation(assertion.result())
      : null;
    return new Results.Failure(
      kind,
      error.getClass().getName(),
      message,
      truncate(trace.toString(), 32768),
      diagnostics
    );
  }

  private static String kind(String testId, Throwable error) {
    if (testId.equals("suite-setup")) return "suite_setup";
    if (error instanceof AssertionError) return "assertion";
    if (error instanceof IOException) return "request";
    return "execution";
  }

  private static String summary(String message) {
    return truncate(
      message
        .lines()
        .filter(line -> !line.isBlank())
        .findFirst()
        .orElse("Check failed"),
      500
    );
  }

  private static String markdown(String message) {
    String text = truncate(message, 16000);
    int fenceLength = BACKTICKS
      .matcher(text)
      .results()
      .mapToInt(match -> match.group().length() + 1)
      .max()
      .orElse(3);
    String fence = "`".repeat(Math.max(3, fenceLength));
    return fence + "text\n" + text + "\n" + fence;
  }

  private static String truncate(String value, int length) {
    return value.length() <= length
      ? value
      : value.substring(0, length) + "\n[truncated]";
  }
}
