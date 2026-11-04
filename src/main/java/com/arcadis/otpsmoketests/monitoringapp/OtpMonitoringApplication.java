package com.arcadis.otpsmoketests.monitoringapp;

import com.arcadis.otpsmoketests.BaseTestSuite;
import com.arcadis.otpsmoketests.configuration.Configuration;
import com.arcadis.otpsmoketests.configuration.ConfigurationLoader;
import com.arcadis.otpsmoketests.results.ResultJson;
import com.arcadis.otpsmoketests.results.ResultSettings;
import com.arcadis.otpsmoketests.results.ResultsApi;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import it.sauronsoftware.cron4j.Scheduler;
import java.time.Clock;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtpMonitoringApplication {

  private static final Logger logger = LoggerFactory.getLogger(
    OtpMonitoringApplication.class
  );

  public static void main(String[] args) {
    try {
      // Load configuration
      String configPath = args.length > 0 ? args[0] : "config.kdl";
      logger.info("Loading configuration from: {}", configPath);

      Configuration config;
      try {
        config = ConfigurationLoader.loadFromFile(configPath);
        logger.info(
          "Configuration loaded successfully with {} deployments",
          config.deploymentsUnderTest().size()
        );
      } catch (Exception e) {
        logger.error("Failed to load configuration from: {}", configPath, e);
        System.exit(1);
        return;
      }

      // Create Micrometer registry for Prometheus
      PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT
      );

      // Set the meter registry for all test suites to use the same registry as the HTTP endpoint
      BaseTestSuite.setMeterRegistry(meterRegistry);

      TestRunner testRunner = new TestRunner(
        meterRegistry,
        config,
        ResultSettings.fromEnvironment(),
        Clock.systemUTC()
      );

      // Start Javalin HTTP server with custom ObjectMapper
      Javalin app = Javalin
        .create(jConfig -> {
          jConfig.jsonMapper(new JavalinJackson(ResultJson.mapper(), false));
          if (
            OtpMonitoringApplication.class.getResource(
                "/web/suite-builder/index.html"
              ) !=
            null
          ) {
            jConfig.staticFiles.add(staticFiles -> {
              staticFiles.hostedPath = "/suite-builder";
              staticFiles.directory = "/web/suite-builder";
              staticFiles.location = Location.CLASSPATH;
            });
          }
        })
        .start(8080);
      ResultsApi.register(app, testRunner.results());
      app.get("/", ctx -> ctx.redirect("/suite-builder/"));
      app.get("/run-tests", ctx -> ctx.json(testRunner.runTestsManually()));
      app.get("/health", ctx -> ctx.json(Map.of("status", "UP")));
      app.get(
        "/metrics",
        ctx -> ctx.result(meterRegistry.scrape()).contentType("text/plain")
      );

      // Start cron scheduler with individual suite intervals
      Scheduler scheduler = new Scheduler();
      testRunner.scheduleTestSuites(scheduler);
      scheduler.start();

      logger.info("OTP Sentinel started on port 8080, metrics at /metrics");

      // Add shutdown hook
      Runtime
        .getRuntime()
        .addShutdownHook(
          new Thread(() -> {
            logger.info("Shutting down...");
            app.stop();
            scheduler.stop();
          })
        );
    } catch (Exception e) {
      logger.error("Failed to start application", e);
      System.exit(1);
    }
  }
}
