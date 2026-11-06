package com.arcadis.otpsmoketests.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A single-writer store: publish to disk before exposing a completed run to readers. */
public final class ResultStore {

  private static final Logger LOG = LoggerFactory.getLogger(ResultStore.class);
  private final Path directory;
  private final Duration retention;
  private final int maxRuns;
  private final Clock clock;
  private final ObjectMapper mapper = ResultJson.mapper();
  private final Map<String, Results.Suite> suites = new TreeMap<>();

  private record StoredRun(String runId, String suiteId, Instant completedAt) {}

  private final Map<String, StoredRun> runs = new HashMap<>();
  private final Map<String, Results.Latest> latest = new HashMap<>();

  public ResultStore(
    Path directory,
    Collection<Results.Suite> suites,
    Duration retention,
    int maxRuns,
    Clock clock
  ) throws IOException {
    if (
      retention.isNegative() ||
      retention.isZero() ||
      maxRuns < Math.max(1, suites.size())
    ) {
      throw new IllegalArgumentException(
        "Retention must be positive and max runs must cover every configured suite"
      );
    }
    this.directory = directory;
    this.retention = retention;
    this.maxRuns = maxRuns;
    this.clock = clock;
    Set<String> metricSuffixes = new HashSet<>();
    for (var suite : suites) {
      if (
        this.suites.putIfAbsent(suite.id(), suite) != null ||
        !metricSuffixes.add(suite.metricSuffix())
      ) {
        throw new IllegalArgumentException(
          "Duplicate suite ID or metric suffix: " + suite.id()
        );
      }
    }
    Files.createDirectories(directory);
    load();
    prune();
  }

  public synchronized List<Results.SuiteSummary> suites() {
    return suites
      .values()
      .stream()
      .map(suite -> {
        var run = latest.get(suite.id());
        return new Results.SuiteSummary(
          suite.id(),
          suite.deploymentName(),
          suite.suiteName(),
          suite.metricSuffix(),
          suite.interval(),
          run == null ? "never_run" : run.status(),
          run == null ? null : run.runId(),
          run == null ? null : run.completedAt()
        );
      })
      .toList();
  }

  public synchronized Optional<Results.Latest> latest(String suiteId) {
    var suite = suites.get(suiteId);
    return suite == null
      ? Optional.empty()
      : Optional.ofNullable(
        latest.getOrDefault(suiteId, Results.Latest.of(suite, null))
      );
  }

  public synchronized Optional<Results.Run> run(String runId)
    throws IOException {
    var run = runs.get(runId);
    if (
      run == null || !suites.containsKey(run.suiteId()) || !retained(run)
    ) return Optional.empty();
    return Optional.of(
      mapper.readValue(file(runId).toFile(), Results.Run.class)
    );
  }

  public synchronized void publish(Results.Run run) throws IOException {
    UUID.fromString(run.runId());
    if (!suites.containsKey(run.suiteId())) throw new IllegalArgumentException(
      "Unknown suite: " + run.suiteId()
    );
    if (runs.containsKey(run.runId())) throw new IllegalArgumentException(
      "Run already exists: " + run.runId()
    );
    Path temporary = Files.createTempFile(directory, "run-", ".tmp");
    try {
      mapper.writeValue(temporary.toFile(), run);
      Files.move(temporary, file(run.runId()), StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temporary);
    }
    index(run);
    try {
      prune();
    } catch (IOException e) {
      LOG.warn("Run was saved, but expired results could not be removed", e);
    }
  }

  private void load() throws IOException {
    try (var files = Files.list(directory)) {
      for (var file : files
        .filter(p -> p.getFileName().toString().endsWith(".json"))
        .toList()) {
        try {
          var run = mapper.readValue(file.toFile(), Results.Run.class);
          UUID.fromString(run.runId());
          if (
            run.schemaVersion() != 1 || !file.equals(file(run.runId()))
          ) throw new IOException("Unsupported run snapshot");
          index(run);
        } catch (IOException | RuntimeException e) {
          LOG.warn("Ignoring invalid run snapshot {}", file, e);
        }
      }
    }
  }

  private void index(Results.Run run) {
    runs.put(
      run.runId(),
      new StoredRun(run.runId(), run.suiteId(), run.completedAt())
    );
    if (!suites.containsKey(run.suiteId())) return;
    latest.merge(
      run.suiteId(),
      Results.Latest.of(suites.get(run.suiteId()), run),
      (previous, next) ->
        next.completedAt().isBefore(previous.completedAt()) ? previous : next
    );
  }

  private boolean isLatest(StoredRun run) {
    var current = latest.get(run.suiteId());
    return current != null && current.runId().equals(run.runId());
  }

  private boolean retained(StoredRun run) {
    return (
      isLatest(run) ||
      !run.completedAt().isBefore(clock.instant().minus(retention))
    );
  }

  private void prune() throws IOException {
    var removable = runs
      .values()
      .stream()
      .filter(run -> !isLatest(run))
      .sorted(Comparator.comparing(StoredRun::completedAt))
      .toList();
    for (var run : removable) {
      if (!retained(run) || runs.size() > maxRuns) {
        Files.deleteIfExists(file(run.runId()));
        runs.remove(run.runId());
      }
    }
  }

  private Path file(String runId) {
    return directory.resolve(runId + ".json");
  }
}
