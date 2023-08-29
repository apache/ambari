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
package org.apache.ambari.metrics.core.timeline.discovery;

import junit.framework.Assert;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.uuid.MetricUuidGenStrategy;
import org.apache.ambari.metrics.core.timeline.uuid.Murmur3HashUuidGenStrategy;
import org.apache.ambari.metrics.core.timeline.uuid.TimelineMetricUuid;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static com.mongodb.util.MyAsserts.assertNotNull;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata.MetricType.GAUGE;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_METADATA_FILTERS;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class TestMetadataSync {
  private MetricUuidGenStrategy uuidGenStrategy = new Murmur3HashUuidGenStrategy();
  @Test
  public void testRefreshMetadataOnWrite() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    PhoenixHBaseAccessor hBaseAccessor = createNiceMock(PhoenixHBaseAccessor.class);

    final TimelineMetricMetadata testMetadata1 = new TimelineMetricMetadata(
      "m1", "a1", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);
    setMetricsUuid(testMetadata1);
    final TimelineMetricMetadata testMetadata2 = new TimelineMetricMetadata(
      "m2", "a2", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);
    setMetricsUuid(testMetadata2);

    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata =
      new HashMap<TimelineMetricMetadataKey, TimelineMetricMetadata>() {{
        put(new TimelineMetricMetadataKey("m1", "a1", null), testMetadata1);
        put(new TimelineMetricMetadataKey("m2", "a2", null), testMetadata2);
      }};

    Map<String, TimelineMetricHostMetadata> hostedApps = new HashMap<String, TimelineMetricHostMetadata>() {{
      put("h1", new TimelineMetricHostMetadata(new HashSet<>(Arrays.asList("a1"))));
      put("h2", new TimelineMetricHostMetadata((new HashSet<>(Arrays.asList("a1", "a2")))));
    }};

    Map<String, Set<String>> hostedInstances = new HashMap<String, Set<String>>() {{
      put("i1", new HashSet<>(Arrays.asList("h1")));
      put("i2", new HashSet<>(Arrays.asList("h1", "h2")));
    }};

    expect(configuration.get("timeline.metrics.service.operation.mode")).andReturn("distributed");
    expect(hBaseAccessor.getTimelineMetricMetadata()).andReturn(metadata);
    expect(hBaseAccessor.getHostedAppsMetadata()).andReturn(hostedApps);
    expect(hBaseAccessor.getInstanceHostsMetdata()).andReturn(hostedInstances);

    replay(configuration, hBaseAccessor);

    TimelineMetricMetadataManager metadataManager = new TimelineMetricMetadataManager(configuration, hBaseAccessor);

    metadataManager.metricMetadataSync = new TimelineMetricMetadataSync(metadataManager);

    metadataManager.metricMetadataSync.run();

    verify(configuration, hBaseAccessor);

    metadata = metadataManager.getMetadataCache();
    Assert.assertEquals(2, metadata.size());
    Assert.assertTrue(metadata.containsKey(new TimelineMetricMetadataKey("m1", "a1", null)));
    Assert.assertTrue(metadata.containsKey(new TimelineMetricMetadataKey("m2", "a2", null)));
    // Check if synced metrics can be found with uuid
    Assert.assertNotNull("metrics not found with testMetadata1 uuid", metadataManager.getMetricFromUuid(testMetadata1.getUuid()));
    Assert.assertNotNull("metrics not found with testMetadata2 uuid", metadataManager.getMetricFromUuid(testMetadata2.getUuid()));

    hostedApps = metadataManager.getHostedAppsCache();
    Assert.assertEquals(2, hostedApps.size());
    Assert.assertEquals(1, hostedApps.get("h1").getHostedApps().size());
    Assert.assertEquals(2, hostedApps.get("h2").getHostedApps().size());

    hostedInstances = metadataManager.getHostedInstanceCache();
    Assert.assertEquals(2, hostedInstances.size());
    Assert.assertEquals(1, hostedInstances.get("i1").size());
    Assert.assertEquals(2, hostedInstances.get("i2").size());

  }

  private void setMetricsUuid(TimelineMetricMetadata tmm) {
    byte[] uuidBytes = uuidGenStrategy.computeUuid(new TimelineClusterMetric(tmm.getMetricName(), tmm.getAppId(),
            tmm.getInstanceId(), tmm.getSeriesStartTime()), TimelineMetricMetadataManager.TIMELINE_METRIC_UUID_LENGTH);
    tmm.setUuid(uuidBytes);
  }
  @Test
  public void testFilterByRegexOnMetricName() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    PhoenixHBaseAccessor hBaseAccessor = createNiceMock(PhoenixHBaseAccessor.class);

    TimelineMetricMetadata metadata1 = new TimelineMetricMetadata(
      "xxx.abc.yyy", "a1", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);
    TimelineMetricMetadata metadata2 = new TimelineMetricMetadata(
      "xxx.cdef.yyy", "a2", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);
    TimelineMetricMetadata metadata3 = new TimelineMetricMetadata(
      "xxx.pqr.zzz", "a3", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);

    expect(configuration.get(TIMELINE_METRIC_METADATA_FILTERS)).andReturn("abc,cde");

    replay(configuration, hBaseAccessor);

    TimelineMetricMetadataManager metadataManager = new TimelineMetricMetadataManager(configuration, hBaseAccessor);

    metadataManager.putIfModifiedTimelineMetricMetadata(metadata1);
    metadataManager.putIfModifiedTimelineMetricMetadata(metadata2);
    metadataManager.putIfModifiedTimelineMetricMetadata(metadata3);

    verify(configuration, hBaseAccessor);

    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata =
      metadataManager.getMetadataCache();

    Assert.assertEquals(1, metadata.size());
    Assert.assertEquals("xxx.pqr.zzz", metadata.keySet().iterator().next().getMetricName());
  }

  @Test
  public void testRefreshHostAppsOnWrite() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    PhoenixHBaseAccessor hBaseAccessor = createNiceMock(PhoenixHBaseAccessor.class);
    Map<String, TimelineMetricHostMetadata> hostedApps = new HashMap<String, TimelineMetricHostMetadata>() {{
      put("host1", new TimelineMetricHostMetadata(new HashSet<>(Arrays.asList("app1", "app2", "app3"))));
    }};

    expect(configuration.get("timeline.metrics.service.operation.mode")).andReturn("distributed");
    expect(hBaseAccessor.getHostedAppsMetadata()).andReturn(hostedApps);
    replay(configuration, hBaseAccessor);

    // register host1 --> (app1,app2)
    TimelineMetricMetadataManager metadataManager = new TimelineMetricMetadataManager(configuration, hBaseAccessor);
    TimelineMetric tm1=new TimelineMetric("metric1","host1","app1",null );
    TimelineMetric tm2=new TimelineMetric("metric2","host1","app2",null );
    byte[] uuid1 = metadataManager.getUuid(tm1, true);
    assertNotNull(uuid1);
    byte[] uuid2 = metadataManager.getUuid(tm2, true);
    assertNotNull(uuid2);
    metadataManager.markSuccessOnSyncHostedAppsMetadata();

    hostedApps = metadataManager.getHostedAppsCache();
    // Before other collector's host app data is synced
    Assert.assertEquals(2, hostedApps.get("host1").getHostedApps().size());

    metadataManager.metricMetadataSync = new TimelineMetricMetadataSync(metadataManager);
    metadataManager.metricMetadataSync.run();

    verify(configuration, hBaseAccessor);

    hostedApps = metadataManager.getHostedAppsCache();
    Assert.assertEquals(1, hostedApps.size());
    // After other collector's host app data is synced
    Assert.assertEquals("Host app list is not synced properly",3, hostedApps.get("host1").getHostedApps().size());
  }

  @Test
  public void testSyncHostUuid() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    PhoenixHBaseAccessor hBaseAccessor = createNiceMock(PhoenixHBaseAccessor.class);
    TimelineMetricHostMetadata hostMeta = new TimelineMetricHostMetadata(new HashSet<>(Arrays.asList("app1", "app2", "app3")));
    byte[] hostUuid = uuidGenStrategy.computeUuid("host1", TimelineMetricMetadataManager.HOSTNAME_UUID_LENGTH);
    hostMeta.setUuid(hostUuid);
    Map<String, TimelineMetricHostMetadata> hostedApps = new HashMap<String, TimelineMetricHostMetadata>() {{
      put("host1", hostMeta);
    }};

    expect(configuration.get("timeline.metrics.service.operation.mode")).andReturn("distributed");
    expect(hBaseAccessor.getHostedAppsMetadata()).andReturn(hostedApps);
    replay(configuration, hBaseAccessor);

    TimelineMetricMetadataManager metadataManager = new TimelineMetricMetadataManager(configuration, hBaseAccessor);
    metadataManager.markSuccessOnSyncHostedAppsMetadata();
    hostedApps = metadataManager.getHostedAppsCache();
    // Before other collector's host app data is synced
    Assert.assertNull(hostedApps.get("host1"));

    metadataManager.metricMetadataSync = new TimelineMetricMetadataSync(metadataManager);
    metadataManager.metricMetadataSync.run();

    verify(configuration, hBaseAccessor);

    hostedApps = metadataManager.getHostedAppsCache();
    Assert.assertEquals(1, hostedApps.size());
    Field field = TimelineMetricMetadataManager.class.getDeclaredField("uuidHostMap");
    field.setAccessible(true);

    Map<TimelineMetricUuid, String> uuidHostMap = (Map<TimelineMetricUuid, String>) field.get(metadataManager);
    TimelineMetricUuid timelineMetricUuid = new TimelineMetricUuid(hostUuid);
    Assert.assertTrue("Host uuid was not synced", uuidHostMap.containsKey(timelineMetricUuid));
  }
}
