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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Verifies the vmagent storage allowlist against packaged telemetry consumers. */
public class PrometheusScrapeContractTest {
  private static final String STACK_ROOT = "/stacks/BIGTOP/3.2.0/services/";
  private static final String DASHBOARD_ROOT = "/metrics/integrations/Linux/dashboards/";
  private static final String SCRAPE_TEMPLATE = STACK_ROOT
      + "VICTORIAMETRICS/package/templates/promscrape.yml.j2";
  private static final String SCRAPE_CONFIG = STACK_ROOT
      + "VICTORIAMETRICS/configuration/victoriametrics-scrape.xml";
  private static final String STORAGE_CONFIG = STACK_ROOT
      + "VICTORIAMETRICS/configuration/victoriametrics.xml";
  private static final String SCRAPE_PARAMS = STACK_ROOT
      + "VICTORIAMETRICS/package/scripts/params.py";
  private static final String SERVICE_SCRIPT = STACK_ROOT
      + "VICTORIAMETRICS/package/scripts/victoriametrics.py";
  private static final String SERVICE_ADVISOR = STACK_ROOT
      + "VICTORIAMETRICS/service_advisor.py";
  private static final String SERVICE_METADATA = STACK_ROOT
      + "VICTORIAMETRICS/metainfo.xml";
  private static final String STACK_PACKAGES = "/stacks/BIGTOP/3.2.0/properties/stack_packages.json";
  private static final Pattern SELECTOR = Pattern.compile(
      "([A-Za-z_:][A-Za-z0-9_:]*)\\{((?:\\$\\{[^}]+}|[^}])*)}");
  private static final Pattern COMPONENT = Pattern.compile(
      "(?:^|,)component=\\\"([^\\\"]+)\\\"");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String[] DASHBOARDS = {
      "HDFS.json",
      "HDFS_NameNode.json",
      "HDFS_DataNode.json",
      "HbaseMaster.json",
      "HbaseRegionServer.json",
      "HIVE_Server2.json",
      "YARN_NodeManager.json",
      "YARN_ResourceManager.json",
      "YARN_ResourceManager_Sys.json"
  };

  private static final Map<String, String> PROFILE_COMPONENTS = new LinkedHashMap<>();

  static {
    PROFILE_COMPONENTS.put("YARN/telemetry-profiles/nodemanager-3.3.json", "NODEMANAGER");
    PROFILE_COMPONENTS.put("HBASE/telemetry-profiles/hbase-master-2.4.json", "HBASE_MASTER");
    PROFILE_COMPONENTS.put("HBASE/telemetry-profiles/hbase-regionserver-2.4.json",
        "HBASE_REGIONSERVER");
    PROFILE_COMPONENTS.put("HIVE/telemetry-profiles/hiveserver2-3.1.json", "HIVE_SERVER");
  }

  @Test
  public void testDashboardAndProfileMetricsPassStorageAllowlist() throws Exception {
    String template = readText(SCRAPE_TEMPLATE);
    String keepRegex = regexInBlock(template,
        "source_labels: [ambari_target, component, __name__]", "action: keep");
    Pattern allowlist = Pattern.compile(keepRegex);

    assertTrue("Host metric prefix must be retained",
        allowlist.matcher("host;;ambari_agent_contract_probe").matches());
    assertFalse("Non-Agent families must not be stored from host targets",
        allowlist.matcher("host;;python_gc_objects_collected_total").matches());

    for (String dashboard : DASHBOARDS) {
      JsonNode definition = readJson(DASHBOARD_ROOT + dashboard);
      for (JsonNode panel : definition.path("configs").path("panels")) {
        for (JsonNode target : panel.path("targets")) {
          verifyDashboardExpression(allowlist, dashboard, target.path("expr").asText());
        }
      }
    }

    for (Map.Entry<String, String> profileEntry : PROFILE_COMPONENTS.entrySet()) {
      JsonNode profile = readJson(STACK_ROOT + profileEntry.getKey());
      for (JsonNode rule : profile.path("rules")) {
        rule.path("attributes").fields().forEachRemaining(attribute -> {
          String metricName = attribute.getValue().path("name").asText();
          String candidate = "component;" + profileEntry.getValue() + ";" + metricName;
          assertTrue("Profile metric is dropped by storage allowlist: " + candidate,
              allowlist.matcher(candidate).matches());
        });
      }
    }

    assertFalse("NameNode Top metrics must not be retained",
        allowlist.matcher("component;NAMENODE;nn_top_user_op_counts_window_ms_60000_count")
            .matches());
    assertFalse("Unqueried native metrics must not be retained",
        allowlist.matcher("component;NAMENODE;fs_namesystem_files_total").matches());
    assertFalse("RE2-incompatible regex extension in storage allowlist", keepRegex.contains("(?"));
  }

  @Test
  public void testHighCardinalitySeriesAreDroppedBeforeLabels() throws Exception {
    String template = readText(SCRAPE_TEMPLATE);
    String perUserDropRegex = regexInBlock(template,
        "source_labels: [component, __name__, user]", "action: drop");
    Pattern perUserDrop = Pattern.compile(perUserDropRegex);

    assertTrue("ResourceManager per-user queue series must be dropped",
        perUserDrop.matcher("RESOURCEMANAGER;queue_metrics_apps_running;alice").matches());
    assertFalse("Aggregate queue series must remain eligible",
        perUserDrop.matcher("RESOURCEMANAGER;queue_metrics_apps_running;").matches());
    assertFalse("Unrelated user-labelled metrics must not match the queue drop",
        perUserDrop.matcher("RESOURCEMANAGER;jvm_metrics_gc_count;alice").matches());

    String labelDropRegex = regexInBlock(template,
        "These Hadoop record tags change independently", "action: labeldrop");
    Pattern labelDrop = Pattern.compile(labelDropRegex);
    for (String label : new String[] {
        "capacityschedulermetrics", "clustermetrics", "enabledecpolicies", "isoutofsync",
        "sessionid", "storageinfo", "totalsynctimes", "zkrmstatestoreopdurations"
    }) {
      assertTrue("Hadoop record label must be removed: " + label,
          labelDrop.matcher(label).matches());
    }
    for (String label : new String[] {
        "hastate", "host", "hostname", "op", "port", "queue", "servername", "user"
    }) {
      assertFalse("Semantic series label must not be removed: " + label,
          labelDrop.matcher(label).matches());
    }

    int userDrop = template.indexOf("source_labels: [component, __name__, user]");
    int allowlist = template.indexOf("source_labels: [ambari_target, component, __name__]");
    int labelDropIndex = template.indexOf("action: labeldrop");
    assertTrue("Per-user series must be dropped before the metric allowlist",
        userDrop >= 0 && userDrop < allowlist);
    assertTrue("Labels must be removed only after series selection",
        allowlist < labelDropIndex);
  }

  @Test
  public void testHttpDiscoveryAndScrapeReplicationContracts() throws Exception {
    String template = readText(SCRAPE_TEMPLATE);
    String config = readText(SCRAPE_CONFIG);
    String storageConfig = readText(STORAGE_CONFIG);
    String params = readText(SCRAPE_PARAMS);
    String service = readText(SERVICE_SCRIPT);
    String advisor = readText(SERVICE_ADVISOR);

    assertFalse("vmagent ignores per-http_sd refresh_interval", template.contains("refresh_interval:"));
    assertTrue("HTTP SD interval must remain a managed configuration property",
        config.contains("<name>http_sd_refresh_interval</name>"));
    assertTrue("HTTP SD interval is not read from managed configuration",
        params.contains("http_sd_refresh_interval = get_property("));
    assertTrue("HTTP SD interval is not passed to vmagent",
        service.contains("-promscrape.httpSDCheckInterval={0}"));
    assertTrue("Scrape replication XML must cap concurrency at two",
        config.contains("<maximum>2</maximum>"));
    assertTrue("Service Advisor must reject replication above Agent concurrency",
        advisor.contains("if scrape_replication > 2:"));
    assertTrue("Runtime validation must reject replication outside 1..2",
        service.contains("if not 1 <= params.vmagent_replication_factor <= 2:"));
    assertTrue("Runtime validation must reject replication above the member count",
        service.contains("if params.vmagent_replication_factor > params.vmagent_members_count:"));

    assertTrue("RF=2 must derive storage deduplication from the scrape interval",
        params.contains("if vmagent_replication_factor > 1\n  else dedup_min_scrape_interval"));
    assertTrue("RF=1 without storage replication must leave deduplication disabled",
        params.contains("replication_factor > 1 or vmagent_replication_factor > 1"));
    assertTrue("Single-node storage must enable deduplication for scrape RF=2",
        service.contains("if params.vmagent_replication_factor > 1:"));
    assertTrue("Cluster storage and query must share the effective deduplication setting",
        occurrences(service, "if params.deduplication_enabled:") == 2);
    assertTrue("All managed storage paths must use the effective deduplication interval",
        occurrences(service, "params.effective_dedup_min_scrape_interval") == 3);
    assertTrue("External storage must receive an explicit deduplication warning",
        advisor.contains("External remote-write storage must deduplicate samples"));
    assertTrue("Storage and scrape replication must be distinguished in managed documentation",
        storageConfig.contains("Replicated VMAGENT scrapes instead use the managed scrape interval"));
  }

  @Test
  public void testMetricsRpmIsNotManagedByBigtopSelect() throws Exception {
    JsonNode stackSelect = readJson(STACK_PACKAGES).path("BIGTOP").path("stack-select");
    String metadata = readText(SERVICE_METADATA);

    assertFalse("The private Ambari Metrics provider is not a bigtop-select component",
        stackSelect.has("VICTORIAMETRICS"));
    assertTrue("VictoriaMetrics service must install the Ambari Metrics provider RPM",
        metadata.contains("<name>ambari-metrics</name>"));
  }

  private void verifyDashboardExpression(Pattern allowlist, String dashboard, String expression) {
    Matcher selectors = SELECTOR.matcher(expression);
    while (selectors.find()) {
      String metricName = selectors.group(1);
      String candidate;
      if (metricName.startsWith("ambari_agent_")) {
        candidate = "host;;" + metricName;
      } else {
        Matcher component = COMPONENT.matcher(selectors.group(2));
        assertTrue("Missing component label for " + metricName + " in " + dashboard,
            component.find());
        candidate = "component;" + component.group(1) + ";" + metricName;
      }
      assertTrue("Dashboard metric is dropped by storage allowlist: " + candidate,
          allowlist.matcher(candidate).matches());
    }
  }

  private String regexInBlock(String text, String anchor, String action) {
    int anchorIndex = text.indexOf(anchor);
    assertTrue("Missing scrape contract anchor: " + anchor, anchorIndex >= 0);
    int actionIndex = text.indexOf(action, anchorIndex);
    assertTrue("Missing scrape contract action after: " + anchor, actionIndex > anchorIndex);
    String block = text.substring(anchorIndex, actionIndex);
    Matcher regex = Pattern.compile("regex: '([^']+)'").matcher(block);
    assertTrue("Missing scrape contract regex after: " + anchor, regex.find());
    return regex.group(1);
  }

  private JsonNode readJson(String resource) throws IOException {
    try (InputStream stream = Files.newInputStream(sourceResource(resource))) {
      return MAPPER.readTree(stream);
    }
  }

  private String readText(String resource) throws IOException {
    return Files.readString(sourceResource(resource), StandardCharsets.UTF_8);
  }

  private Path sourceResource(String resource) {
    Path path = Paths.get("src/main/resources", resource.substring(1));
    assertTrue("Missing source resource " + path, Files.isRegularFile(path));
    return path;
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
}
