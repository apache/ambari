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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricsIgniteCache;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_COLLECTOR_IGNITE_NODES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getRoundedCheckPointTimeMillis;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getTimeSlices;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimelineMetricConfiguration.class)

@PowerMockIgnore("javax.management.*")
public class TimelineMetricClusterAggregatorSecondWithCacheSourceTest {

  private static TimelineMetricsIgniteCache timelineMetricsIgniteCache;
  @BeforeClass
  public static void setupConf() throws Exception {
    TimelineMetricConfiguration conf = new TimelineMetricConfiguration(new
        Configuration(), new Configuration());
    mockStatic(TimelineMetricConfiguration.class);
    expect(TimelineMetricConfiguration.getInstance()).andReturn(conf).anyTimes();
    conf.getMetricsConf().set(TIMELINE_METRICS_COLLECTOR_IGNITE_NODES, "localhost");
    replayAll();

    timelineMetricsIgniteCache = new TimelineMetricsIgniteCache();
  }

  @Test
  public void testLiveHostCounterMetrics() throws Exception {
    long aggregatorInterval = 120000;
    long sliceInterval = 30000;

    Configuration configuration = new Configuration();

    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getMetadataCacheValue((TimelineMetricMetadataKey) anyObject())).andReturn(null).anyTimes();
    replay(metricMetadataManagerMock);

    TimelineMetricClusterAggregatorSecondWithCacheSource secondAggregator = new TimelineMetricClusterAggregatorSecondWithCacheSource(
        METRIC_AGGREGATE_SECOND, metricMetadataManagerMock, null, configuration, null,
        aggregatorInterval, 2, "false", "", "", aggregatorInterval,
        sliceInterval, null, timelineMetricsIgniteCache);

    long now = System.currentTimeMillis();
    long startTime = now - 120000;
    long seconds = 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> metricsFromCache = new HashMap<>();
    metricsFromCache.put(new TimelineClusterMetric("m1", "a1", "i1",startTime + 15 * seconds),
        new MetricClusterAggregate(1.0, 2, 1.0, 1.0, 1.0));
    metricsFromCache.put(new TimelineClusterMetric("m2", "a2", "i1",startTime + 18 * seconds),
        new MetricClusterAggregate(1.0, 5, 1.0, 1.0, 1.0));

    List<Long[]> timeslices = getTimeSlices(startTime, startTime + 120*seconds, 30*seconds);
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregates = secondAggregator.aggregateMetricsFromMetricClusterAggregates(metricsFromCache, timeslices);

    Assert.assertNotNull(aggregates);

    MetricClusterAggregate a1 = null, a2 = null;

    for (Map.Entry<TimelineClusterMetric, MetricClusterAggregate> m : aggregates.entrySet()) {
      if (m.getKey().getMetricName().equals("live_hosts") && m.getKey().getAppId().equals("a1")) {
        a1 = m.getValue();
      }
      if (m.getKey().getMetricName().equals("live_hosts") && m.getKey().getAppId().equals("a2")) {
        a2 = m.getValue();
      }
    }

    Assert.assertNotNull(a1);
    Assert.assertNotNull(a2);
    Assert.assertEquals(2d, a1.getSum());
    Assert.assertEquals(5d, a2.getSum());
  }
}
