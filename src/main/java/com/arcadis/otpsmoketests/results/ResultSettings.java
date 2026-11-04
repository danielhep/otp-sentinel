package com.arcadis.otpsmoketests.results;

import java.nio.file.Path;
import java.time.Duration;

public record ResultSettings(Path directory, Duration retention, int maxRuns) {
  public static ResultSettings fromEnvironment() {
    var env = System.getenv();
    return new ResultSettings(
      Path.of(env.getOrDefault("SENTINEL_RESULTS_DIR", "results")),
      Duration.ofDays(
        Long.parseLong(env.getOrDefault("SENTINEL_RESULTS_RETENTION_DAYS", "7"))
      ),
      Integer.parseInt(env.getOrDefault("SENTINEL_RESULTS_MAX_RUNS", "5000"))
    );
  }
}
