package com.arcadis.otpsmoketests.results;

import java.time.Duration;

public record ResultSettings(Duration retention, int maxRuns) {
  public static ResultSettings defaults() {
    return new ResultSettings(Duration.ofDays(7), 5000);
  }
}
