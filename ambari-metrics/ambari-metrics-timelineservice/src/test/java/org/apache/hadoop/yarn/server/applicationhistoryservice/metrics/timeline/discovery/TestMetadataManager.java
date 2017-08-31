/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.AbstractMiniHBaseClusterTest;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TestMetadataManager extends AbstractMiniHBaseClusterTest {
  TimelineMetricMetadataManager metadataManager;

  @Before
  public void insertDummyRecords() throws IOException, SQLException, URISyntaxException {

    final long now = System.currentTimeMillis();

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("dummy_metric1");
    metric1.setHostName("dummy_host1");
    metric1.setStartTime(now - 1000);
    metric1.setAppId("dummy_app1");
    metric1.setType("Integer");
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric1);
    TimelineMetric metric2 = new TimelineMetric();
    metric2.setMetricName("dummy_metric2");
    metric2.setHostName("dummy_host2");
    metric2.setStartTime(now - 1000);
    metric2.setAppId("dummy_app2");
    metric2.setType("Integer");
    metric2.setInstanceId("instance2");
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric2);

    Configuration metricsConf = createNiceMock(Configuration.class);
    expect(metricsConf.get("timeline.metrics.service.operation.mode")).andReturn("distributed").anyTimes();
    replay(metricsConf);

    // Initialize new manager
    metadataManager = new TimelineMetricMetadataManager(metricsConf, hdb);
    hdb.setMetadataInstance(metadataManager);

    hdb.insertMetricRecordsWithMetadata(metadataManager, timelineMetrics, true);
  }

  @Test(timeout = 180000)
  public void testSaveMetricsMetadata() throws Exception {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> cachedData = metadataManager.getMetadataCache();

    Assert.assertNotNull(cachedData);
    Assert.assertEquals(2, cachedData.size());
    TimelineMetricMetadataKey key1 = new TimelineMetricMetadataKey("dummy_metric1", "dummy_app1", null);
    TimelineMetricMetadataKey key2 = new TimelineMetricMetadataKey("dummy_metric2", "dummy_app2", "instance2");
    TimelineMetricMetadata value1 = new TimelineMetricMetadata("dummy_metric1",
      "dummy_app1", null, null, "Integer", 1L, true, true);
    TimelineMetricMetadata value2 = new TimelineMetricMetadata("dummy_metric2",
      "dummy_app2", "instance2", null, "Integer", 1L, true, true);

    Assert.assertEquals(value1, cachedData.get(key1));
    Assert.assertEquals(value2, cachedData.get(key2));

    TimelineMetricMetadataSync syncRunnable = new TimelineMetricMetadataSync(metadataManager);
    syncRunnable.run();

    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> savedData =
      hdb.getTimelineMetricMetadata();

    Assert.assertNotNull(savedData);
    Assert.assertEquals(2, savedData.size());
    Assert.assertEquals(value1, savedData.get(key1));
    Assert.assertEquals(value2, savedData.get(key2));

    Map<String, TimelineMetricHostMetadata> cachedHostData = metadataManager.getHostedAppsCache();
    Map<String, TimelineMetricHostMetadata> savedHostData = metadataManager.getHostedAppsFromStore();
    Assert.assertEquals(cachedData.size(), savedData.size());
    Assert.assertEquals("dummy_app1", cachedHostData.get("dummy_host1").getHostedApps().keySet().iterator().next());
    Assert.assertEquals("dummy_app2", cachedHostData.get("dummy_host2").getHostedApps().keySet().iterator().next());
    Assert.assertEquals("dummy_app1", savedHostData.get("dummy_host1").getHostedApps().keySet().iterator().next());
    Assert.assertEquals("dummy_app2", savedHostData.get("dummy_host2").getHostedApps().keySet().iterator().next());

    Map<String, Set<String>> cachedHostInstanceData = metadataManager.getHostedInstanceCache();
    Map<String, Set<String>> savedHostInstanceData = metadataManager.getHostedInstancesFromStore();
    Assert.assertEquals(cachedHostInstanceData.size(), savedHostInstanceData.size());
    Assert.assertEquals("dummy_host2", cachedHostInstanceData.get("instance2").iterator().next());
  }

  @Test
  public void testGenerateUuidFromMetric() throws SQLException {

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("regionserver.Server.blockCacheExpressHitPercent");
    timelineMetric.setAppId("hbase");
    timelineMetric.setHostName("avijayan-ams-2.openstacklocal");
    timelineMetric.setInstanceId("test1");

    byte[] uuid = metadataManager.getUuid(timelineMetric);
    Assert.assertNotNull(uuid);
    Assert.assertEquals(uuid.length, 20);

    byte[] uuidWithoutHost = metadataManager.getUuid(new TimelineClusterMetric(timelineMetric.getMetricName(), timelineMetric.getAppId(), timelineMetric.getInstanceId(), -1));
    Assert.assertNotNull(uuidWithoutHost);
    Assert.assertEquals(uuidWithoutHost.length, 16);

    TimelineMetric metric2 = metadataManager.getMetricFromUuid(uuid);
    Assert.assertEquals(metric2, timelineMetric);
    TimelineMetric metric3 = metadataManager.getMetricFromUuid(uuidWithoutHost);
    Assert.assertEquals(metric3.getMetricName(), timelineMetric.getMetricName());
    Assert.assertEquals(metric3.getAppId(), timelineMetric.getAppId());
    Assert.assertEquals(metric3.getInstanceId(), timelineMetric.getInstanceId());
    Assert.assertEquals(metric3.getHostName(), null);

    String metricName1 = metadataManager.getMetricNameFromUuid(uuid);
    Assert.assertEquals(metricName1, "regionserver.Server.blockCacheExpressHitPercent");
    String metricName2 = metadataManager.getMetricNameFromUuid(uuidWithoutHost);
    Assert.assertEquals(metricName2, "regionserver.Server.blockCacheExpressHitPercent");
  }

  @Test
  public void testWildcardSanitization() throws IOException, SQLException, URISyntaxException {
    // Initialize new manager
    metadataManager = new TimelineMetricMetadataManager(new Configuration(), hdb);
    final long now = System.currentTimeMillis();

    TimelineMetrics timelineMetrics = new TimelineMetrics();

    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("dummy_m1");
    metric1.setHostName("dummy_host1");
    metric1.setStartTime(now - 1000);
    metric1.setAppId("dummy_app1");
    metric1.setType("Integer");
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric1);

    TimelineMetric metric2 = new TimelineMetric();
    metric2.setMetricName("dummy_m2");
    metric2.setHostName("dummy_host2");
    metric2.setStartTime(now - 1000);
    metric2.setAppId("dummy_app2");
    metric2.setType("Integer");
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric2);

    TimelineMetric metric3 = new TimelineMetric();
    metric3.setMetricName("gummy_3");
    metric3.setHostName("dummy_3h");
    metric3.setStartTime(now - 1000);
    metric3.setAppId("dummy_app3");
    metric3.setType("Integer");
    metric3.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric3);

    Configuration metricsConf = new Configuration();
    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();
    replay(configuration);

    hdb.insertMetricRecordsWithMetadata(metadataManager, timelineMetrics, true);

    List<byte[]> uuids = metadataManager.getUuids(Collections.singletonList("dummy_m%"),
      Collections.singletonList("dummy_host2"), "dummy_app1", null);
    Assert.assertTrue(uuids.size() == 2);

    uuids = metadataManager.getUuids(Collections.singletonList("dummy_m%"),
      Collections.singletonList("dummy_host%"), "dummy_app2", null);
    Assert.assertTrue(uuids.size() == 4);

    Collection<String> metrics = Arrays.asList("dummy_m%", "dummy_3", "dummy_m2");
    List<String> hosts = Arrays.asList("dummy_host%", "dummy_3h");
    uuids = metadataManager.getUuids(metrics, hosts, "dummy_app2", null);
    Assert.assertTrue(uuids.size() == 9);
  }


}
