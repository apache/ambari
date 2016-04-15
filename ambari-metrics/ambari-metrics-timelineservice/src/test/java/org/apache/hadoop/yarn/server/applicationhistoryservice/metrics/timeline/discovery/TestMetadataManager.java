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

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.AbstractMiniHBaseClusterTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TestMetadataManager extends AbstractMiniHBaseClusterTest {
  TimelineMetricMetadataManager metadataManager;

  @Before
  public void insertDummyRecords() throws IOException, SQLException {
    // Initialize new manager
    metadataManager = new TimelineMetricMetadataManager(hdb, new Configuration());
    final long now = System.currentTimeMillis();

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("dummy_metric1");
    metric1.setHostName("dummy_host1");
    metric1.setTimestamp(now);
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
    metric2.setTimestamp(now);
    metric2.setStartTime(now - 1000);
    metric2.setAppId("dummy_app2");
    metric2.setType("Integer");
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
      put(now - 300, 3.0);
    }});
    timelineMetrics.getMetrics().add(metric2);

    hdb.insertMetricRecordsWithMetadata(metadataManager, timelineMetrics);
  }

  @Test(timeout = 180000)
  public void testSaveMetricsMetadata() throws Exception {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> cachedData = metadataManager.getMetadataCache();

    Assert.assertNotNull(cachedData);
    Assert.assertEquals(2, cachedData.size());
    TimelineMetricMetadataKey key1 = new TimelineMetricMetadataKey("dummy_metric1", "dummy_app1");
    TimelineMetricMetadataKey key2 = new TimelineMetricMetadataKey("dummy_metric2", "dummy_app2");
    TimelineMetricMetadata value1 = new TimelineMetricMetadata("dummy_metric1",
      "dummy_app1", "Integer", null, 1L, true);
    TimelineMetricMetadata value2 = new TimelineMetricMetadata("dummy_metric2",
      "dummy_app2", "Integer", null, 1L, true);

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

    Map<String, Set<String>> cachedHostData = metadataManager.getHostedAppsCache();
    Map<String, Set<String>> savedHostData = metadataManager.getPersistedHostedAppsData();
    Assert.assertEquals(cachedData.size(), savedData.size());
    Assert.assertEquals("dummy_app1", cachedHostData.get("dummy_host1").iterator().next());
    Assert.assertEquals("dummy_app2", cachedHostData.get("dummy_host2").iterator().next());
    Assert.assertEquals("dummy_app1", savedHostData.get("dummy_host1").iterator().next());
    Assert.assertEquals("dummy_app2", savedHostData.get("dummy_host2").iterator().next());
  }


}
