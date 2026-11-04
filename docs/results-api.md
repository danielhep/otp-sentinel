# Results API

The API serves completed test runs. Reading results does not execute tests. Both
scheduled execution and `/run-tests` publish through the same report store.

## Endpoints

| Request | Response |
| --- | --- |
| `GET /api/v1/suites` | Configured suites and their latest status, run ID and completion time |
| `GET /api/v1/suites/{suiteId}/latest` | Compact latest-run summary and failure rows |
| `GET /api/v1/runs/{runId}` | Full immutable run, including all checks and captured journey requests |

All successful reads return HTTP 200, including failing test results. Unknown
suites and unknown or expired run IDs return 404. Responses use `Cache-Control:
no-store`; clients refresh the latest view explicitly.

For example, this configuration provides a stable suite ID independent of its
display names:

```kdl
deployment name="Hope Link Prod" url="https://example.com" {
  test-suite id="hopelink-prod.basic" name="Hope Link Basic" class="HopeLink" interval="*/10 * * * *"
}
```

Suite IDs must be unique and contain letters, digits, periods, underscores or
hyphens, starting with a letter or digit. Without an explicit ID, Sentinel
generates a deterministic UUID from the deployment and suite names; renaming
either then changes the ID. Give deployed suites explicit IDs before renaming
them. Metric suffixes remain unchanged and are included in the suite listing
so existing metric-based dashboard variables can resolve the corresponding ID.
Duplicate IDs or metric suffixes are rejected at startup.

## Latest result

`GET /api/v1/suites/hopelink-prod.basic/latest` returns a response like:

```json
{
  "suiteId": "hopelink-prod.basic",
  "deploymentName": "Hope Link Prod",
  "suiteName": "Hope Link Basic",
  "runId": "03b25a91-7e6f-4e2a-a439-7e850b81ca52",
  "status": "failed",
  "startedAt": "2026-11-03T18:20:00Z",
  "completedAt": "2026-11-03T18:20:02Z",
  "durationMs": 2100,
  "counts": {"total": 12, "passed": 11, "failed": 1},
  "failures": [{
    "suiteId": "hopelink-prod.basic",
    "runId": "03b25a91-7e6f-4e2a-a439-7e850b81ca52",
    "completedAt": "2026-11-03T18:20:02Z",
    "testId": "airport",
    "testName": "Downtown to Airport",
    "kind": "assertion",
    "durationMs": 320,
    "summary": "At least one itinerary must match [failed]",
    "detailsMarkdown": "```text\nExpected route 560; returned route 999\n```"
  }]
}
```

The example diagnostic text is abbreviated. Actual assertion reports include
expectations, successful and unsuccessful alternatives, actual legs, candidate
matches and missing criteria. Markdown safely fences the diagnostic text and
is limited to 16,000 characters; the complete message and structured assertion
diagnostics remain in the full run. Stack traces are limited to 32,768 characters.

The latest status is `passed`, `failed`, `error` (suite setup or empty discovery),
or `never_run`. Before the first completed run, the run ID, timestamps, duration
and counts are null, and failures is empty. Empty discovery reports zero checks
and a suite-level failure row whose test ID is null. Constructor/discovery errors
retain the runner's synthetic `suite-setup` test outcome.

While a run executes, readers continue seeing the previous completed snapshot.
The store writes the new snapshot atomically before exposing it. The next passing
run clears the current failure rows. Completion timestamps always describe the
actual run; consumers should show their age so stale passing results are visible.
Runs of the same suite execute serially, including manual/scheduled overlap.

## Full run

`GET /api/v1/runs/{runId}` contains the summary fields plus `schemaVersion: 1`,
`trigger` (`manual` or `scheduled`) and `tests`. Each test has:

- `testId`, `testName`, `status`, `durationMs` and `detailsMarkdown`;
- `requests`: the deployment endpoint, time zone, resolved request parameters
  (including journey time, locations, modes and request overrides), and projected
  response itineraries;
- `failure`, or null for successful checks.

Failures include `kind`, `exceptionType`, `message`, `stackTrace` and `diagnostics`.
Kinds distinguish `assertion`, `request`, `suite_setup` and `execution` errors.
Only itinerary assertion errors have structured plan-match diagnostics. Ordinary
assertion errors still have diagnostic text. Request failures retain their
resolved request with a null response.

The diagnostic object preserves the expectation description, match outcome,
matching mode, expected legs, itinerary criteria, full evaluated plan, itinerary
results and recursive `children`. Itinerary results include errors, unexpected
legs and requirement results. Each requirement retains its status, assigned leg
and candidate legs with matched and missing criteria. Fields are projected into
intentional JSON types rather than serializing Java exceptions or client objects.

Requests made through the suite's OTP client are captured, including direct
`plan()` and `timedPlan()` calls. Requests made during suite initialization are
excluded from per-test reports. Other OTP operations (for example route inventory)
have ordinary test error details but no journey request snapshot.

## Storage and retention

| Environment variable | Default |
| --- | --- |
| `SENTINEL_RESULTS_DIR` | `results` (`/app/results` in Docker) |
| `SENTINEL_RESULTS_RETENTION_DAYS` | `7` |
| `SENTINEL_RESULTS_MAX_RUNS` | `5000` |

Mount the results directory to persistent storage. The store reloads reports and
last-run metric values at startup. Full plans remain on disk; memory holds the
history index and the latest compact summary for each suite.

Expired runs are removed on publication and startup, and are unavailable through
the API after expiry. Oldest historical runs are also removed to enforce the run
count. Each configured suite's latest result remains available regardless of age,
so an old result can be diagnosed rather than looking like a fresh installation.
The maximum count must accommodate at least one run per configured suite.
Interrupted temporary writes and invalid snapshots are ignored during startup;
invalid files are logged for inspection. Use one Sentinel process per results
directory.

## Grafana

Use the Infinity data source's JSON backend parser against the container-network
address, for example `http://otp-monitoring:8080/api/v1/suites`. Map the suite
listing's metric suffix to its ID when preserving existing Prometheus selectors.
Select `failures` from the latest view for a table, render `detailsMarkdown` as a
Markdown cell, and link its run ID to a run-details dashboard. Render full-run
request and diagnostic objects using inspectable JSON cells.

The configurations repository provisions the data source and dashboards. It uses
Grafana's server-side data source proxy; the Sentinel API does not need a public
reverse-proxy route. The API has no built-in authentication, so keep it on the
private monitoring network or provide authentication at a proxy before exposing
it. Prometheus remains responsible for numeric metrics and alert evaluation,
including `otp_tests_<suffix>_last_completed_timestamp_seconds` for freshness.
