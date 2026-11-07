package com.arcadis.otpsmoketests.configuration;

import com.arcadis.otpsmoketests.BaseTestSuite;
import com.arcadis.otpsmoketests.results.ResultSettings;
import java.util.Collection;

public record Configuration(
  Collection<DeploymentUnderTest> deploymentsUnderTest,
  ResultSettings results
) {
  public record DeploymentUnderTest(
    String name,
    String url,
    Collection<TestSuite> suites
  ) {}

  public record TestSuite(
    String name,
    Class<BaseTestSuite> clazz,
    String interval,
    String id
  ) {
    public TestSuite(String name, Class<BaseTestSuite> clazz, String interval) {
      this(name, clazz, interval, null);
    }
  }
}
