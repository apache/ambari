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

package org.apache.ambari.metrics.core.timeline.uuid;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

public class MetricUuidGenStrategyTest {


  private static List<String> apps = Arrays.asList("namenode",
    "datanode", "hbase_master", "hbase_regionserver", "kafka_broker", "nimbus", "ams-hbase",
    "accumulo", "nodemanager", "resourcemanager", "ambari_server", "HOST", "timeline_metric_store_watcher",
    "jobhistoryserver", "hiveserver2", "hivemetastore", "applicationhistoryserver", "amssmoketestfake", "llapdaemon");

  private static Map<String, Set<String>> metricSet  = new HashMap<>();

  @BeforeClass
  public static void init() {
    metricSet  = new HashMap<>(populateMetricWhitelistFromFile());
  }

  @Test
  @Ignore
  public void testHashBasedUuid() throws SQLException {
    testMetricCollisionsForUuidGenStrategy(new HashBasedUuidGenStrategy(), 16);
  }

  @Test
  @Ignore
  public void testHashBasedUuidForAppIds() throws SQLException {
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
  @Ignore
  public void testHashBasedUuidForHostnames() throws SQLException {
    testHostCollisionsForUuidGenStrategy(new HashBasedUuidGenStrategy(), 16);
  }


  @Test
  public void testMD5BasedUuid() throws SQLException {
    testMetricCollisionsForUuidGenStrategy(new MD5UuidGenStrategy(), 16);

  }

  @Test
  public void testMD5BasedUuidForHostnames() throws SQLException {
    testHostCollisionsForUuidGenStrategy(new MD5UuidGenStrategy(), 16);
  }


  @Test
  public void testMD5ConsistentHashing() throws SQLException, InterruptedException {
    testConsistencyForUuidGenStrategy(new MD5UuidGenStrategy(), 16);
  }


  @Test
  public void testMurmur3HashUuid() throws SQLException {
    testMetricCollisionsForUuidGenStrategy(new Murmur3HashUuidGenStrategy(), 16);
  }

  @Test
  public void testMurmur3HashingBasedUuidForHostnames() throws SQLException {
    testHostCollisionsForUuidGenStrategy(new Murmur3HashUuidGenStrategy(), 4);
  }

  @Test
  public void testMurmur3ConsistentHashing() throws SQLException, InterruptedException {
    testConsistencyForUuidGenStrategy(new Murmur3HashUuidGenStrategy(), 4);
  }

  private void testMetricCollisionsForUuidGenStrategy(MetricUuidGenStrategy strategy, int uuidLength) {
    Map<TimelineMetricUuid, TimelineClusterMetric> uuids = new HashMap<>();
    for (String app : metricSet.keySet()) {
      Set<String> metrics = metricSet.get(app);
      for (String m : metrics) {
        TimelineClusterMetric metric = new TimelineClusterMetric(m, app, null, -1l);
        byte[] uuid = strategy.computeUuid(metric, uuidLength);
        Assert.assertNotNull(uuid);
        Assert.assertTrue(uuid.length == uuidLength);
        TimelineMetricUuid uuidStr = new TimelineMetricUuid(uuid);
        Assert.assertFalse(uuids.containsKey(uuidStr) && !uuids.containsValue(metric));
        uuids.put(uuidStr, metric);
      }
    }
  }


  private void testHostCollisionsForUuidGenStrategy(MetricUuidGenStrategy strategy, int uuidLength) {
    Map<TimelineMetricUuid, String> uuids = new HashMap<>();

    List<String> hosts = new ArrayList<>();
    String hostPrefix = "TestHost.";
    String hostSuffix = ".ambari.apache.org";

    for (int i=0; i<=2000; i++) {
      hosts.add(hostPrefix + i + hostSuffix);
    }

    for (String host : hosts) {
      byte[] uuid = strategy.computeUuid(host, uuidLength);
      Assert.assertNotNull(uuid);
      Assert.assertTrue(uuid.length == uuidLength);
      TimelineMetricUuid uuidStr = new TimelineMetricUuid(uuid);
      Assert.assertFalse(uuids.containsKey(uuidStr));
      uuids.put(uuidStr, host);
    }
  }

  private void testConsistencyForUuidGenStrategy(MetricUuidGenStrategy strategy, int length) throws InterruptedException {
    String key = "TestString";

    byte[] uuid = strategy.computeUuid(key, length);
    Assert.assertNotNull(uuid);
    Assert.assertTrue(uuid.length == length);

    for (int i = 0; i<100; i++) {
      byte[] uuid2 = strategy.computeUuid(key, length);
      Assert.assertNotNull(uuid2);
      Assert.assertTrue(uuid2.length == length);
      Assert.assertArrayEquals(uuid, uuid2);
      Thread.sleep(10);
    }
  }

  private static Map<String, Set<String>> populateMetricWhitelistFromFile() {

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
      if (appId.startsWith("hbase")) {
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
