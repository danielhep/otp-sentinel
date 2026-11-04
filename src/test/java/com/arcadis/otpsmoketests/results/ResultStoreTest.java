package com.arcadis.otpsmoketests.results;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResultStoreTest {

  @TempDir
  Path directory;

  private final Instant now = Instant.parse("2026-11-03T18:30:00Z");
  private final List<Results.Suite> suites = List.of(
    new Results.Suite(
      "a",
      "Deployment A",
      "Basic",
      "Deployment_A_Basic",
      "*/10 * * * *"
    ),
    new Results.Suite(
      "b",
      "Deployment B",
      "Basic",
      "Deployment_B_Basic",
      "*/10 * * * *"
    )
  );

  @Test
  void historySurvivesRestartAndRespectsAgeAndCountWhileKeepingEachSuitesLatest()
    throws Exception {
    var store = store();
    var oldA = run("a", now.minus(Duration.ofDays(10)));
    var staleB = run("b", now.minus(Duration.ofDays(9)));
    store.publish(oldA);
    store.publish(staleB);
    store.publish(run("a", now.minusSeconds(30)));
    assertTrue(store.run(oldA.runId()).isEmpty());
    assertFalse(Files.exists(directory.resolve(oldA.runId() + ".json")));
    var countEvicted = run("a", now.minusSeconds(20));
    store.publish(countEvicted);
    var previous = run("a", now.minusSeconds(10));
    store.publish(previous);
    var latest = run("a", now);
    store.publish(latest);
    assertTrue(store.run(countEvicted.runId()).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> store.publish(latest));
    Files.writeString(directory.resolve("incomplete.tmp"), "interrupted write");
    Files.writeString(directory.resolve("invalid.json"), "{broken");
    var restarted = store();
    assertEquals(latest, restarted.run(latest.runId()).orElseThrow());
    assertEquals(previous, restarted.run(previous.runId()).orElseThrow());
    assertEquals(staleB, restarted.run(staleB.runId()).orElseThrow());
    assertEquals(
      staleB.completedAt(),
      restarted.latest("b").orElseThrow().completedAt()
    );
    try (var files = Files.list(directory)) {
      assertEquals(
        3,
        files
          .filter(p ->
            p.getFileName().toString().matches("[0-9a-f-]{36}\\.json")
          )
          .count()
      );
    }
  }

  private ResultStore store() throws Exception {
    return new ResultStore(
      directory,
      suites,
      Duration.ofDays(7),
      3,
      Clock.fixed(now, ZoneOffset.UTC)
    );
  }

  private Results.Run run(String suiteId, Instant completedAt) {
    return new Results.Run(
      1,
      suiteId,
      "Deployment " + suiteId,
      "Basic",
      UUID.randomUUID().toString(),
      "scheduled",
      "passed",
      completedAt.minusSeconds(1),
      completedAt,
      1000,
      new Results.Counts(1, 1, 0),
      List.of(
        new Results.Check(
          "healthy",
          "Healthy check",
          "passed",
          1000,
          List.of(),
          null,
          ""
        )
      ),
      List.of()
    );
  }
}
