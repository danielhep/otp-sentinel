package com.arcadis.otpsmoketests.results;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

public final class ResultsApi {

  private ResultsApi() {}

  public static void register(Javalin app, ResultStore store) {
    app.before("/api/v1/*", ctx -> ctx.header("Cache-Control", "no-store"));
    app.get("/api/v1/suites", ctx -> ctx.json(store.suites()));
    app.get(
      "/api/v1/suites/{suiteId}/latest",
      ctx ->
        ctx.json(
          store
            .latest(ctx.pathParam("suiteId"))
            .orElseThrow(() -> new NotFoundResponse("Unknown suite"))
        )
    );
    app.get(
      "/api/v1/runs/{runId}",
      ctx ->
        ctx.json(
          store
            .run(ctx.pathParam("runId"))
            .orElseThrow(() -> new NotFoundResponse("Unknown or expired run"))
        )
    );
  }
}
