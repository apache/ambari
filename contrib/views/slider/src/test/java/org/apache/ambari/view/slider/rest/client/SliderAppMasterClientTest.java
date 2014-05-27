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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SliderAppMasterClientTest {

  @Test
  public void testSliderClientClassAvailability() {
    SliderAppMasterClient client = new SliderAppMasterClient("http://tmpurl.org");
    Map<String, Metric> relevantMetric = new HashMap<String, Metric>();
    Map<String, String> jmxProperties = new HashMap<String, String>();
    Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();
    relevantMetric.put("metricAverageLoad",
                      new Metric("Hadoop:service=HBase,name=Master,sub=Server.averageLoad", true, false));
    relevantMetric.put("DeadRegionServers",
                      new Metric("Hadoop:service=HBase,name=Master,sub=Server.numDeadRegionServers", true, false));
    relevantMetric.put("ClusterId",
                      new Metric("Hadoop:service=HBase,name=Master,sub=Server.tag.clusterId", true, false));
    relevantMetric.put("IsActiveMaster",
                      new Metric("Hadoop:service=HBase,name=Master,sub=Server.tag.isActiveMaster", true, false));
    relevantMetric.put("peakUsageCommitted",
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

    client.addJmxProperties(jmxProperties, categories, relevantMetric);
    Assert.assertEquals(jmxProperties.size(), 4);
  }

  @Test
  public void testMetricMatchers() throws Exception {
    Metric m1 = new Metric("a_b.c", true, false);
    Assert.assertEquals(m1.getKeyName(), "a_b");
    List<List<String>> matchers = m1.getMatchers();
    Assert.assertEquals(matchers.size(), 1);
    Assert.assertEquals(matchers.get(0).size(), 1);
    Assert.assertEquals(matchers.get(0).get(0), "c");

    m1 = new Metric("a_b.c.d", true, false);
    Assert.assertEquals(m1.getKeyName(), "a_b");
    matchers = m1.getMatchers();
    Assert.assertEquals(matchers.size(), 2);
    Assert.assertEquals(matchers.get(0).size(), 2);
    Assert.assertEquals(matchers.get(0).get(0), "c");
    Assert.assertEquals(matchers.get(0).get(1), "d");
    Assert.assertEquals(matchers.get(1).size(), 1);
    Assert.assertEquals(matchers.get(1).get(0), "c.d");

    m1 = new Metric("a_b.c.d.e", true, false);
    Assert.assertEquals(m1.getKeyName(), "a_b");
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
}
