/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.uuid;

import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimelineMetricUuidManagerTest {


  private List<String> apps = Arrays.asList("namenode",
    "datanode", "master_hbase", "slave_hbase", "kafka_broker", "nimbus", "ams-hbase",
    "accumulo", "nodemanager", "resourcemanager", "ambari_server", "HOST", "timeline_metric_store_watcher",
    "jobhistoryserver", "hiveserver2", "hivemetastore", "applicationhistoryserver", "amssmoketestfake");

  private Map<String, Set<String>> metricSet  = new HashMap<>(populateMetricWhitelistFromFile());

  @Test
  public void testHashBasedUuidForMetricName() throws SQLException {

    MetricUuidGenStrategy strategy = new HashBasedUuidGenStrategy();
    Map<String, TimelineClusterMetric> uuids = new HashMap<>();
    for (String app : metricSet.keySet()) {
      Set<String> metrics = metricSet.get(app);
      for (String metric : metrics) {
        TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(metric, app, null, -1l);
        byte[] uuid = strategy.computeUuid(timelineClusterMetric, 16);
        Assert.assertNotNull(uuid);
        Assert.assertTrue(uuid.length == 16);
        String uuidStr = new String(uuid);
        Assert.assertFalse(uuids.containsKey(uuidStr) && !uuids.containsValue(timelineClusterMetric));
        uuids.put(uuidStr, timelineClusterMetric);
      }
    }
  }

  @Test
  public void testHaseBasedUuidForAppIds() throws SQLException {

    MetricUuidGenStrategy strategy = new HashBasedUuidGenStrategy();
    Map<String, TimelineClusterMetric> uuids = new HashMap<>();
    for (String app : metricSet.keySet()) {
      TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric("TestMetric", app, null, -1l);
      byte[] uuid = strategy.computeUuid(timelineClusterMetric, 16);
      String uuidStr = new String(uuid);
      Assert.assertFalse(uuids.containsKey(uuidStr) && !uuids.containsValue(timelineClusterMetric));
      uuids.put(uuidStr, timelineClusterMetric);
    }
  }

  @Test
  public void testHashBasedUuidForHostnames() throws SQLException {

    MetricUuidGenStrategy strategy = new HashBasedUuidGenStrategy();
    Map<String, String> uuids = new HashMap<>();

    List<String> hosts = new ArrayList<>();
    String hostPrefix = "TestHost.";
    String hostSuffix = ".ambari.apache.org";

    for (int i=0; i<=2000; i++) {
      hosts.add(hostPrefix + i + hostSuffix);
    }

    for (String host : hosts) {
      byte[] uuid = strategy.computeUuid(host, 4);
      Assert.assertNotNull(uuid);
      Assert.assertTrue(uuid.length == 4);
      String uuidStr = new String(uuid);
      Assert.assertFalse(uuids.containsKey(uuidStr));
      uuids.put(uuidStr, host);
    }
  }


  @Test
  public void testRandomUuidForWhitelistedMetrics() throws SQLException {

    MetricUuidGenStrategy strategy = new RandomUuidGenStrategy();
    Map<String, String> uuids = new HashMap<>();
    for (String app : metricSet.keySet()) {
      Set<String> metrics = metricSet.get(app);
      for (String metric : metrics) {
        byte[] uuid = strategy.computeUuid(new TimelineClusterMetric(metric, app, null, -1l), 16);
        Assert.assertNotNull(uuid);
        Assert.assertTrue(uuid.length == 16);
        String uuidStr = new String(uuid);
        Assert.assertFalse(uuids.containsKey(uuidStr) && !uuids.containsValue(metric));
        uuids.put(uuidStr, metric);
      }
    }
  }

  public Map<String, Set<String>> populateMetricWhitelistFromFile() {


    Map<String, Set<String>> metricSet = new HashMap<String, Set<String>>();
    FileInputStream fstream = null;
    Set<String> hbaseMetrics = new HashSet<>();
    BufferedReader br = null;
    String strLine;
    for (String appId : apps) {
      URL fileUrl = ClassLoader.getSystemResource("metrics_def/" + appId.toUpperCase() + ".dat");

      Set<String> metricsForApp = new HashSet<>();
      try {
        fstream = new FileInputStream(fileUrl.getPath());
        br = new BufferedReader(new InputStreamReader(fstream));
        while ((strLine = br.readLine()) != null)   {
          strLine = strLine.trim();
          metricsForApp.add(strLine);
        }
      } catch (Exception ioEx) {
        System.out.println("Metrics for AppId " + appId + " not found.");
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (IOException e) {
          }
        }

        if (fstream != null) {
          try {
            fstream.close();
          } catch (IOException e) {
          }
        }
      }
      metricsForApp.add("live_hosts");
      if (appId.equals("master_hbase") || appId.equals("slave_hbase")) {
        hbaseMetrics.addAll(metricsForApp);
      } else {
        metricSet.put(appId, metricsForApp);
      }
      System.out.println("Found " + metricsForApp.size() + " metrics for appId = " + appId);
    }
    metricSet.put("hbase", hbaseMetrics);
    return metricSet;
  }
}
