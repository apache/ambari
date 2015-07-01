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

package org.apache.ambari.view.slider.rest.client;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SliderAppMasterClientTest {

  @Test
  public void testSliderClientClassAvailability() {
    SliderAppMasterClient client = new SliderAppMasterClient("http://tmpurl.org", null);
    Map<String, Metric> metrics = new HashMap<String, Metric>();
    Map<String, String> jmxProperties = new HashMap<String, String>();
    Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();
    metrics.put("metricAverageLoad",
                new Metric("Hadoop:service=HBase,name=Master,sub=Server.averageLoad", true, false));
    metrics.put("DeadRegionServers",
                new Metric("Hadoop:service=HBase,name=Master,sub=Server.numDeadRegionServers", true, false));
    metrics.put("ClusterId",
                new Metric("Hadoop:service=HBase,name=Master,sub=Server.tag.clusterId", true, false));
    metrics.put("IsActiveMaster",
                new Metric("Hadoop:service=HBase,name=Master,sub=Server.tag.isActiveMaster", true, false));
    metrics.put("peakUsageCommitted",
                new Metric("java#lang:type=MemoryPool,name=Code Cache.PeakUsage.committed", true, false));

    Map<String, Object> masterServer = new HashMap<String, Object>();
    masterServer.put("averageLoad", "0.1");
    masterServer.put("numDeadRegionServers", "1");
    masterServer.put("tag.clusterId", "11");
    categories.put("Hadoop:service=HBase,name=Master,sub=Server", masterServer);
    Map<String, Object> memPool = new HashMap<String, Object>();
    Map<String, Object> peakUsage = new HashMap<String, Object>();
    peakUsage.put("committed", 354);
    peakUsage.put("uncommitted", 356);
    memPool.put("PeakUsage", peakUsage);
    memPool.put("SomeOther", "other");
    categories.put("java.lang:type=MemoryPool,name=Code Cache", memPool);

    SliderAppJmxHelper.addJmxPropertiesFromBeans(jmxProperties, categories, metrics);
    Assert.assertEquals(jmxProperties.size(), 4);
  }

  @Test
  public void testMetricMatchers() throws Exception {
    Metric m1 = new Metric("a_b.c", true, false);
    Assert.assertEquals(m1.getJmxBeanKeyName(), "a_b");
    List<List<String>> matchers = m1.getMatchers();
    Assert.assertEquals(matchers.size(), 1);
    Assert.assertEquals(matchers.get(0).size(), 1);
    Assert.assertEquals(matchers.get(0).get(0), "c");

    m1 = new Metric("a_b.c.d", true, false);
    Assert.assertEquals(m1.getJmxBeanKeyName(), "a_b");
    matchers = m1.getMatchers();
    Assert.assertEquals(matchers.size(), 2);
    Assert.assertEquals(matchers.get(0).size(), 2);
    Assert.assertEquals(matchers.get(0).get(0), "c");
    Assert.assertEquals(matchers.get(0).get(1), "d");
    Assert.assertEquals(matchers.get(1).size(), 1);
    Assert.assertEquals(matchers.get(1).get(0), "c.d");

    m1 = new Metric("a_b.c.d.e", true, false);
    Assert.assertEquals(m1.getJmxBeanKeyName(), "a_b");
    matchers = m1.getMatchers();
    Assert.assertEquals(matchers.size(), 4);
    Assert.assertEquals(matchers.get(0).size(), 3);
    Assert.assertEquals(matchers.get(0).get(1), "d");
    Assert.assertEquals(matchers.get(0).get(2), "e");
    Assert.assertEquals(matchers.get(2).size(), 2);
    Assert.assertEquals(matchers.get(2).get(0), "c.d");
    Assert.assertEquals(matchers.get(2).get(1), "e");
    Assert.assertEquals(matchers.get(3).size(), 1);
    Assert.assertEquals(matchers.get(3).get(0), "c.d.e");
  }

  @Test
  public void testReadMetricsFromJson() throws Exception {
    String jsonJmx = "{\n"
                     + "\"tasks.total\": 28,\n"
                     + "\"slots.total\": 1,\n"
                     + "\"slots.free\": 0,\n"
                     + "\"supervisors\": 1,\n"
                     + "\"executors.total\": 28,\n"
                     + "\"slots.used\": 1,\n"
                     + "\"topologies\": 1,\n"
                     + "\"nimbus.uptime\": 2026\n"
                     + "}";
    InputStream stream = new ByteArrayInputStream(jsonJmx.getBytes("UTF-8"));
    Map<String, Metric> metrics = new HashMap<String, Metric>();
    Map<String, String> jmxProperties = new HashMap<String, String>();
    metrics.put("FreeSlots", new Metric("$['slots.free']", true, false));
    metrics.put("Tasks", new Metric("$['tasks.total']", true, false));
    metrics.put("Executors", new Metric("$['executors.total']", true, false));
    metrics.put("Items", new Metric("['items']", true, false));
    SliderAppJmxHelper.extractMetricsFromJmxJson(stream, "jmxurl", jmxProperties, metrics);
    Assert.assertEquals(jmxProperties.size(), 3);
    Assert.assertEquals(jmxProperties.get("FreeSlots"), "0");
    Assert.assertEquals(jmxProperties.get("Tasks"), "28");
    Assert.assertEquals(jmxProperties.get("Executors"), "28");
  }

  @Test
  public void testReadMetricsFromXml() throws Exception {
    String jsonJmx = "<stats>\n"
                     + "<masterGoalState>NORMAL</masterGoalState>\n"
                     + "<masterState>NORMAL</masterState>\n"
                     + "<badTabletServers></badTabletServers>\n"
                     + "<tabletServersShuttingDown></tabletServersShuttingDown>\n"
                     + "<unassignedTablets>0</unassignedTablets>\n"
                     + "<deadTabletServers></deadTabletServers>\n"
                     + "<deadLoggers></deadLoggers>\n"
                     + "<tables>\n"
                     + "<table>\n"
                     + "<tablename>!METADATA</tablename>\n"
                     + "<tableId>!0</tableId>\n"
                     + "<tableState>ONLINE</tableState>\n"
                     + "<tablets>3</tablets>\n"
                     + "<onlineTablets>3</onlineTablets>\n"
                     + "<recs>49</recs>\n"
                     + "<recsInMemory>24</recsInMemory>\n"
                     + "<ingest>1.1271868150075986E-4</ingest>\n"
                     + "<ingestByteRate>0.00475332746606865</ingestByteRate>\n"
                     + "<query>0.014071304698085596</query>\n"
                     + "<queryByteRate>0.014071304698085596</queryByteRate>\n"
                     + "<majorCompactions>\n"
                     + "<running>0</running>\n"
                     + "<queued>0</queued>\n"
                     + "</majorCompactions>\n"
                     + "</table>\n"
                     + "</tables>\n"
                     + "<totals>\n"
                     + "<ingestrate>0.0016737652847869573</ingestrate>\n"
                     + "<queryrate>0.014071304698085596</queryrate>\n"
                     + "<diskrate>0.0</diskrate>\n"
                     + "<numentries>554</numentries>\n"
                     + "</totals>\n"
                     + "</stats>";
    InputStream stream = new ByteArrayInputStream(jsonJmx.getBytes("UTF-8"));
    Map<String, Metric> metrics = new HashMap<String, Metric>();
    Map<String, String> jmxProperties = new HashMap<String, String>();
    metrics.put("masterGoalState", new Metric("/stats/masterGoalState", true, false));
    metrics.put("masterState", new Metric("/stats/masterState", true, false));
    metrics.put("totals_diskrate", new Metric("/stats/totals/diskrate", true, false));
    metrics.put("totals_diskaccess", new Metric("/stats/totals/diskaccess", true, false));
    metrics.put("badTabletServers", new Metric("/stats/badTabletServers", true, false));
    SliderAppJmxHelper.extractMetricsFromJmxXML(stream, "jmxurl", jmxProperties, metrics);
    Assert.assertEquals(jmxProperties.size(), 5);
    Assert.assertEquals(jmxProperties.get("masterGoalState"), "NORMAL");
    Assert.assertEquals(jmxProperties.get("masterState"), "NORMAL");
    Assert.assertEquals(jmxProperties.get("totals_diskrate"), "0.0");
    Assert.assertEquals(jmxProperties.get("totals_diskaccess"), "");
    Assert.assertEquals(jmxProperties.get("badTabletServers"), "");
  }
}
