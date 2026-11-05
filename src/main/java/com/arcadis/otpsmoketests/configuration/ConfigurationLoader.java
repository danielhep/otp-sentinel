package com.arcadis.otpsmoketests.configuration;

import com.arcadis.otpsmoketests.BaseTestSuite;
import com.arcadis.otpsmoketests.results.ResultSettings;
import dev.kdl.KdlDocument;
import dev.kdl.KdlNode;
import dev.kdl.parse.KdlParseException;
import dev.kdl.parse.KdlParser;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationLoader {

  public static Configuration loadFromFile(String filePath)
    throws IOException, KdlParseException {
    String content = Files.readString(Path.of(filePath));
    return loadFromString(content);
  }

  public static Configuration loadFromString(String kdlContent)
    throws KdlParseException, IOException {
    KdlParser parser = KdlParser.v2();
    KdlDocument document = parser.parse(kdlContent);
    List<Configuration.DeploymentUnderTest> deployments = new ArrayList<>();

    for (KdlNode node : document.nodes()) {
      if ("deployment".equals(node.name())) {
        deployments.add(parseDeployment(node));
      }
    }

    ResultSettings results = parseResults(document);
    long suiteCount = deployments
      .stream()
      .mapToLong(d -> d.suites().size())
      .sum();
    if (results.maxRuns() < suiteCount) {
      throw new IllegalArgumentException(
        "results max-runs must cover every configured suite (" +
        suiteCount +
        ")"
      );
    }
    return new Configuration(deployments, results);
  }

  private static ResultSettings parseResults(KdlDocument document) {
    var nodes = document
      .nodes()
      .stream()
      .filter(node -> "results".equals(node.name()))
      .toList();
    if (nodes.size() > 1) {
      throw new IllegalArgumentException("Only one results node is allowed");
    }
    return nodes
      .stream()
      .findFirst()
      .map(ConfigurationLoader::parseResultSettings)
      .orElseGet(ResultSettings::defaults);
  }

  private static ResultSettings parseResultSettings(KdlNode node) {
    var defaults = ResultSettings.defaults();
    return new ResultSettings(
      Duration.ofDays(
        readPositiveInteger(
          node,
          "retention-days",
          defaults.retention().toDays(),
          36_500
        )
      ),
      (int) readPositiveInteger(
        node,
        "max-runs",
        defaults.maxRuns(),
        Integer.MAX_VALUE
      )
    );
  }

  private static long readPositiveInteger(
    KdlNode node,
    String property,
    long defaultValue,
    long maximum
  ) {
    return node
      .getProperty(property)
      .map(value -> {
        if (
          !(value.value() instanceof BigInteger number) ||
          number.signum() <= 0 ||
          number.compareTo(BigInteger.valueOf(maximum)) > 0
        ) {
          throw new IllegalArgumentException(
            "results " +
            property +
            " must be an integer between 1 and " +
            maximum
          );
        }
        return number.longValueExact();
      })
      .orElse(defaultValue);
  }

  private static Configuration.DeploymentUnderTest parseDeployment(
    KdlNode deploymentNode
  ) {
    String name = deploymentNode.getProperty("name").get().value().toString();
    String url = deploymentNode.getProperty("url").get().value().toString();

    List<Configuration.TestSuite> suites = new ArrayList<>();

    for (KdlNode childNode : deploymentNode.children()) {
      if ("test-suite".equals(childNode.name())) {
        suites.add(parseTestSuite(childNode));
      }
    }

    return new Configuration.DeploymentUnderTest(name, url, suites);
  }

  private static Configuration.TestSuite parseTestSuite(KdlNode testSuiteNode) {
    String name = testSuiteNode.getProperty("name").get().value().toString();
    String className = testSuiteNode
      .getProperty("class")
      .get()
      .value()
      .toString();
    String interval = testSuiteNode
      .getProperty("interval")
      .get()
      .value()
      .toString();

    Class<BaseTestSuite> clazz = resolveTestSuiteClass(className);

    String id = testSuiteNode
      .getProperty("id")
      .map(value -> value.value().toString())
      .orElse(null);
    if (id != null && !id.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
      throw new IllegalArgumentException(
        "Suite id must contain only letters, digits, periods, underscores or hyphens"
      );
    }
    return new Configuration.TestSuite(name, clazz, interval, id);
  }

  @SuppressWarnings("unchecked")
  private static Class<BaseTestSuite> resolveTestSuiteClass(String className) {
    try {
      String fullClassName;
      if (className.contains(".")) {
        fullClassName = className;
      } else {
        fullClassName = "com.arcadis.otpsmoketests.tests." + className;
        if (!className.endsWith("TestSuite")) {
          fullClassName += "TestSuite";
        }
      }

      Class<?> clazz = Class.forName(fullClassName);
      if (BaseTestSuite.class.isAssignableFrom(clazz)) {
        return (Class<BaseTestSuite>) clazz;
      } else {
        throw new IllegalArgumentException(
          "Class " + fullClassName + " does not extend BaseTestSuite"
        );
      }
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
        "Test suite class not found: " + className,
        e
      );
    }
  }
}
