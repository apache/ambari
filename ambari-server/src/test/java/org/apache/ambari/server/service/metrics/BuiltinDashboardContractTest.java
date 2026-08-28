/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.service.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Verifies packaged PromQL against the Agent telemetry metric contract. */
public class BuiltinDashboardContractTest {
  private static final String DASHBOARD_ROOT = "/metrics/integrations/Linux/dashboards/";
  private static final Pattern SELECTOR = Pattern.compile(
      "([A-Za-z_:][A-Za-z0-9_:]*)\\{((?:\\$\\{[^}]+}|[^}])*)}");
  private static final Pattern RANGE_FUNCTION = Pattern.compile(
      "(?:rate|increase)\\(\\s*([A-Za-z_:][A-Za-z0-9_:]*)"
          + "\\{(?:\\$\\{[^}]+}|[^}])*}\\[\\$__rate_interval]\\s*\\)");
  private static final Pattern FIXED_RANGE = Pattern.compile("\\[[0-9]+[smhdwy]]");
  private static final Pattern LEGACY_METRIC = Pattern.compile("(?:^|[^a-z])(?:hadoop|hbase)_[A-Z]");
  private static final Pattern LEGACY_COMPONENT = Pattern.compile(
      "component=\\\\?\\\"(?:Hadoop|Yarn|Hbase)");
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Set<String> RAW_COUNTER_GAUGES = Set.of(
      "startup_progress_elapsed_time");
  private static final Set<String> SUPPORTED_UNITS = Set.of(
      "", "bytesIEC", "bytesSecIEC", "cps", "percent", "percentUnit", "reqps", "seconds");

  private static final Map<String, DashboardContract> DASHBOARDS = new LinkedHashMap<>();
  private static final String[] PROFILE_RESOURCES = {
      "stacks/BIGTOP/3.2.0/services/YARN/telemetry-profiles/nodemanager-3.3.json",
      "stacks/BIGTOP/3.2.0/services/HBASE/telemetry-profiles/hbase-master-2.4.json",
      "stacks/BIGTOP/3.2.0/services/HBASE/telemetry-profiles/hbase-regionserver-2.4.json",
      "stacks/BIGTOP/3.2.0/services/HIVE/telemetry-profiles/hiveserver2-3.1.json"
  };

  static {
    DASHBOARDS.put("HDFS.json", new DashboardContract("HDFS", "NAMENODE", 23));
    DASHBOARDS.put("HDFS_NameNode.json", new DashboardContract("HDFS", "NAMENODE", 17));
    DASHBOARDS.put("HDFS_DataNode.json", new DashboardContract("HDFS", "DATANODE", 34));
    DASHBOARDS.put("HbaseMaster.json", new DashboardContract("HBASE", "HBASE_MASTER", 25));
    DASHBOARDS.put("HbaseRegionServer.json", new DashboardContract("HBASE", "HBASE_REGIONSERVER", 26));
    DASHBOARDS.put("HIVE_Server2.json", new DashboardContract("HIVE", "HIVE_SERVER", 14));
    DASHBOARDS.put("YARN_NodeManager.json", new DashboardContract("YARN", "NODEMANAGER", 20));
    DASHBOARDS.put("YARN_ResourceManager.json", new DashboardContract("YARN", "RESOURCEMANAGER", 30));
    DASHBOARDS.put("YARN_ResourceManager_Sys.json", new DashboardContract("YARN", "RESOURCEMANAGER", 22));
  }

  @Test
  public void testDashboardQueriesMatchExporterNamesTypesAndLabels() throws Exception {
    Map<String, String> metricTypes = loadNativeMetricTypes();
    loadProfileMetricTypes(metricTypes);

    for (Map.Entry<String, DashboardContract> entry : DASHBOARDS.entrySet()) {
      String fileName = entry.getKey();
      DashboardContract contract = entry.getValue();
      JsonNode definition = readJson(DASHBOARD_ROOT + fileName);
      JsonNode configs = definition.path("configs");
      assertTrue("Missing configs in " + fileName, configs.isObject());

      String serializedConfigs = configs.toString();
      assertFalse("Legacy metric name remains in " + fileName,
          LEGACY_METRIC.matcher(serializedConfigs).find());
      assertFalse("Legacy component label remains in " + fileName,
          LEGACY_COMPONENT.matcher(serializedConfigs).find());

      int queryCount = 0;
      for (JsonNode panel : configs.path("panels")) {
        verifyPanelPresentation(fileName, panel);
        for (JsonNode target : panel.path("targets")) {
          String expression = target.path("expr").asText();
          if (expression.isBlank()) {
            continue;
          }
          queryCount++;
          verifyExpression(fileName, expression, contract, metricTypes);
        }
      }
      assertEquals("Unexpected query count in " + fileName,
          contract.expectedQueries, queryCount);
    }
  }

  private void verifyPanelPresentation(String fileName, JsonNode panel) {
    String panelName = panel.path("name").asText();
    String unit = panel.path("options").path("standardOptions").path("util").asText("");
    Set<String> dimensions = new HashSet<>();
    assertTrue("Unsupported display unit " + unit + " in " + fileName,
        SUPPORTED_UNITS.contains(unit));
    assertFalse("Panel override must not replace the display unit in " + fileName,
        panel.path("overrides").toString().contains("\"util\""));
    if (panelName.endsWith("ratio") || panelName.equals("Queue capacity")) {
      assertEquals("Ratio panel must use a 0..1 display unit in " + fileName,
          "percentUnit", unit);
    }

    for (JsonNode target : panel.path("targets")) {
      String expression = target.path("expr").asText();
      if (!expression.isBlank()) {
        dimensions.add(inferDimension(panelName, expression));
        assertFalse("Missing legend in " + fileName,
            target.path("legend").asText().isBlank());
        if (expression.contains("_mb{")) {
          assertTrue("MiB metric must be converted to bytes in " + fileName,
              expression.contains("* 1048576"));
          assertEquals("Converted byte metric must use bytesIEC in " + fileName,
              "bytesIEC", unit);
        }
      }
    }
    assertTrue("Panel mixes physical dimensions in " + fileName + ": " + panelName,
        dimensions.size() <= 1);
  }

  private String inferDimension(String panelName, String expression) {
    if (panelName.endsWith("ratio") || panelName.equals("Queue capacity")
        || panelName.equals("HDFS usage")) {
      return "ratio";
    }
    if (expression.contains("rate(")) {
      return "rate";
    }
    if (expression.contains("* 1048576") || expression.contains("_bytes{")) {
      return "bytes";
    }
    return "count";
  }

  private void verifyExpression(String fileName, String expression, DashboardContract contract,
      Map<String, String> metricTypes) {
    assertFalse("Fixed range selector in " + fileName + ": " + expression,
        FIXED_RANGE.matcher(expression).find());

    Matcher selectors = SELECTOR.matcher(expression);
    int selectorCount = 0;
    while (selectors.find()) {
      selectorCount++;
      String metricName = selectors.group(1);
      String labels = selectors.group(2);
      assertTrue("Missing metric contract for " + metricName + " in " + fileName,
          metricTypes.containsKey(metricName));
      assertTrue("Missing cluster scope in " + expression,
          labels.contains("cluster=\"${cluster}\""));
      if (fileName.equals("HDFS.json") && metricName.startsWith("fs_namesystem_")) {
        assertTrue("FSNamesystem gauge must select the active NameNode in " + expression,
            labels.contains("hastate=\"active\""));
      }
      if (metricName.startsWith("ambari_agent_")) {
        assertTrue("Host metric is not restricted to host targets in " + expression,
            labels.contains("ambari_target=\"host\""));
      } else {
        assertTrue("Missing service scope in " + expression,
            labels.contains("service=\"" + contract.service + "\""));
        assertTrue("Missing component scope in " + expression,
            labels.contains("component=\"" + contract.component + "\""));
      }
      if ("counter".equals(metricTypes.get(metricName))
          && !RAW_COUNTER_GAUGES.contains(metricName)) {
        assertTrue("Counter must use rate/increase in " + expression,
            expression.contains("rate(" + metricName + "{")
                || expression.contains("increase(" + metricName + "{"));
      }
      if (fileName.equals("YARN_ResourceManager.json")
          && metricName.startsWith("queue_metrics_apps_")
          && "counter".equals(metricTypes.get(metricName))) {
        assertTrue("ResourceManager failover counters must sum instance rates in " + expression,
            expression.contains("sum(rate(" + metricName + "{"));
      }
    }
    assertTrue("Expression has no metric selector in " + fileName + ": " + expression,
        selectorCount > 0);

    int rangeFunctionCount = occurrences(expression, "rate(")
        + occurrences(expression, "increase(");
    Matcher rangeFunctions = RANGE_FUNCTION.matcher(expression);
    int matchedRangeFunctions = 0;
    while (rangeFunctions.find()) {
      matchedRangeFunctions++;
      String metricName = rangeFunctions.group(1);
      assertEquals("Range function requires a counter metric in " + expression,
          "counter", metricTypes.get(metricName));
    }
    assertEquals("rate/increase must use [$__rate_interval] in " + expression,
        rangeFunctionCount, matchedRangeFunctions);
  }

  private Map<String, String> loadNativeMetricTypes() throws IOException {
    Map<String, String> result = new HashMap<>();
    try (InputStream stream = requiredResource(
        "/metrics/contracts/prometheus-exporter-types.txt");
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) {
          continue;
        }
        String[] fields = value.split("\\s+");
        assertEquals("Invalid metric contract line: " + line, 2, fields.length);
        result.put(fields[0], fields[1]);
      }
    }
    return result;
  }

  private void loadProfileMetricTypes(Map<String, String> metricTypes) throws IOException {
    for (String resource : PROFILE_RESOURCES) {
      JsonNode profile = readSourceJson(resource);
      for (JsonNode rule : profile.path("rules")) {
        rule.path("attributes").fields().forEachRemaining(attribute -> {
          String name = attribute.getValue().path("name").asText();
          String type = attribute.getValue().path("type").asText();
          assertFalse("Profile metric name is missing in " + resource, name.isBlank());
          assertTrue("Unsupported profile metric type in " + resource,
              type.equals("counter") || type.equals("gauge"));
          String previous = metricTypes.put(name, type);
          assertTrue("Conflicting type for " + name,
              previous == null || previous.equals(type));
          if (name.endsWith("_ratio")) {
            assertEquals("Percentage source must be normalized to a 0..1 ratio for " + name,
                0.01, attribute.getValue().path("scale").asDouble(), 0.000001);
          }
        });
      }
    }
  }

  private JsonNode readJson(String resource) throws IOException {
    try (InputStream stream = requiredResource(resource)) {
      return MAPPER.readTree(stream);
    }
  }

  private JsonNode readSourceJson(String resource) throws IOException {
    String basedir = System.getProperty("basedir", System.getProperty("user.dir"));
    Path source = Path.of(basedir, "src", "main", "resources", resource);
    try (InputStream stream = Files.newInputStream(source)) {
      return MAPPER.readTree(stream);
    }
  }

  private InputStream requiredResource(String resource) {
    InputStream stream = getClass().getResourceAsStream(resource);
    assertNotNull("Missing test resource " + resource, stream);
    return stream;
  }

  private int occurrences(String value, String needle) {
    int count = 0;
    int offset = 0;
    while ((offset = value.indexOf(needle, offset)) >= 0) {
      count++;
      offset += needle.length();
    }
    return count;
  }

  private static final class DashboardContract {
    private final String service;
    private final String component;
    private final int expectedQueries;

    private DashboardContract(String service, String component, int expectedQueries) {
      this.service = service;
      this.component = component;
      this.expectedQueries = expectedQueries;
    }
  }
}
