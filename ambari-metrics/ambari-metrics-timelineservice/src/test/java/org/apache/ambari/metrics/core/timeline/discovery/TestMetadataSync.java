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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata.MetricType.GAUGE;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_METADATA_FILTERS;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class TestMetadataSync {
  @Test
  public void testRefreshMetadataOnWrite() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    PhoenixHBaseAccessor hBaseAccessor = createNiceMock(PhoenixHBaseAccessor.class);

    final TimelineMetricMetadata testMetadata1 = new TimelineMetricMetadata(
      "m1", "a1", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);
    final TimelineMetricMetadata testMetadata2 = new TimelineMetricMetadata(
      "m2", "a2", null, "", GAUGE.name(), System.currentTimeMillis(), true, false);

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

    hostedApps = metadataManager.getHostedAppsCache();
    Assert.assertEquals(2, hostedApps.size());
    Assert.assertEquals(1, hostedApps.get("h1").getHostedApps().size());
    Assert.assertEquals(2, hostedApps.get("h2").getHostedApps().size());

    hostedInstances = metadataManager.getHostedInstanceCache();
    Assert.assertEquals(2, hostedInstances.size());
    Assert.assertEquals(1, hostedInstances.get("i1").size());
    Assert.assertEquals(2, hostedInstances.get("i2").size());

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
}
