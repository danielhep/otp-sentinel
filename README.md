# OTP Sentinel

Continuous journey testing and monitoring for OpenTripPlanner. Define expected
trip results, run checks against live OTP deployments on a schedule, and expose
test results and response times through Prometheus metrics. The included suite
builder lets you create and edit JSON test suites in your browser.

Copy the example configuration and set the deployment URL to your OTP instance:

```sh
cp config.example.kdl config.kdl
```

The local `config.kdl` is excluded from Git. The container reads it from
`/app/config.kdl`; it is mounted read only and is not included in the image.

```sh
docker compose -f compose.example.yaml up --build
```

Test results and assertion failures are written to the console logs. The
application is available on port 8080, with `/health`, `/metrics`, and `/run-tests`
for manually running all suites.

JSON suites and extension rules are documented in [JSON test suites](docs/json-test-suites.md).

## Results API

Completed scheduled and manual executions are saved as JSON reports. Read them
without executing checks:

```text
GET /api/v1/suites
GET /api/v1/suites/{suiteId}/latest
GET /api/v1/runs/{runId}
```

The latest view includes failure rows with formatted diagnostics for Grafana.
Individual runs also include every test outcome, resolved journey requests,
returned itineraries and nested assertion diagnostics. Historical reports remain
unchanged when a later execution passes.

Reports are stored in `results/` by default. Mount `/app/results` to persistent
storage when using Docker; the example Compose configuration includes a named
volume. Retention defaults to seven days and 5,000 runs, retaining each suite's
latest completed run even when it becomes stale. See the
[results API documentation](docs/results-api.md) for response examples,
configuration and Grafana integration.

## Test suite editor

The React/Vite SPA builds and edits schema v2 JSON suites, with reusable mapped
locations, request defaults, and nested expectations. Build the UI and runner
together with Node.js 24/npm installed:

```sh
mvn -Pweb-ui clean package
```

Open `/suite-builder/` on the monitoring server (port 8080). Docker builds include
the UI automatically. For frontend development, run `npm ci` and `npm run dev`
in `src/main/web`. See the [editor documentation](src/main/web/README.md) for
workflows and build details. Export your work before closing the page; suites
stay in browser memory until downloaded.

## Building

The runner requires the companion `otp-java-client` **3.0.0-SNAPSHOT**. Build and
install that checkout locally before building this project:

```sh
cd ../otp-java-client
mvn -Dtest='*Test,!IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dmaven.javadoc.skip=true install
cd ../otp-sentinel
mvn test package
```

The build produces `target/otp-sentinel-1.0-SNAPSHOT.jar`.

A clean CI or Docker build needs this snapshot in its configured artifact
repository or a published version containing these changes, followed by an
update to `otp.client.version` in `pom.xml`.
