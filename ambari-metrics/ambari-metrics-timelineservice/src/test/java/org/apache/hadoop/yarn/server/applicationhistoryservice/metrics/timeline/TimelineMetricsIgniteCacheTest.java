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

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;


import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_APP_ID;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_COLLECTOR_IGNITE_NODES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getRoundedCheckPointTimeMillis;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimelineMetricConfiguration.class)

@PowerMockIgnore("javax.management.*")
public class TimelineMetricsIgniteCacheTest {
  private static TimelineMetricsIgniteCache timelineMetricsIgniteCache;
  @BeforeClass
  public static void setupConf() throws Exception {
    TimelineMetricConfiguration conf = new TimelineMetricConfiguration(new
      Configuration(), new Configuration());
    mockStatic(TimelineMetricConfiguration.class);
    expect(TimelineMetricConfiguration.getInstance()).andReturn(conf).anyTimes();
    conf.getMetricsConf().set(CLUSTER_AGGREGATOR_APP_IDS, "appIdForHostsAggr");
    conf.getMetricsConf().set(TIMELINE_METRICS_COLLECTOR_IGNITE_NODES, "localhost");
    replayAll();

    timelineMetricsIgniteCache = new TimelineMetricsIgniteCache();
  }

  @Test
  public void putEvictMetricsFromCacheSlicesMerging() throws Exception {
    long cacheSliceIntervalMillis = 30000L;

    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class))).andReturn(new byte[16]).once();
    replay(metricMetadataManagerMock);

    long startTime = getRoundedCheckPointTimeMillis(System.currentTimeMillis(), cacheSliceIntervalMillis);

    long seconds = 1000;
    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();
    /*

    0        +30s      +60s
    |         |         |
     (1)(2)(3) (4)(5)(6)  h1

    */
    // Case 1 : data points are distributed equally, no values are lost, single host.
    metricValues.put(startTime + 4*seconds, 1.0);
    metricValues.put(startTime + 14*seconds, 2.0);
    metricValues.put(startTime + 24*seconds, 3.0);
    metricValues.put(startTime + 34*seconds, 4.0);
    metricValues.put(startTime + 44*seconds, 5.0);
    metricValues.put(startTime + 54*seconds, 6.0);

    TimelineMetric timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setStartTime(metricValues.firstKey());
    timelineMetric.addMetricValues(metricValues);

    Collection<TimelineMetric> timelineMetrics = new ArrayList<>();
    timelineMetrics.add(timelineMetric);
    timelineMetricsIgniteCache.putMetrics(timelineMetrics, metricMetadataManagerMock);
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMap = timelineMetricsIgniteCache.evictMetricAggregates(startTime, startTime + 120*seconds);

    Assert.assertEquals(aggregateMap.size(), 2);
    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(timelineMetric.getMetricName(),
      timelineMetric.getAppId(), timelineMetric.getInstanceId(), startTime + 30*seconds);

    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(2.0, aggregateMap.get(timelineClusterMetric).getSum());

    timelineClusterMetric.setTimestamp(startTime + 2*30*seconds);
    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(5.0, aggregateMap.get(timelineClusterMetric).getSum());

    metricValues.clear();
    timelineMetrics.clear();

    /*

    0        +30s      +60s
    |         |         |
     (1)(2)(3) (4)(5)(6)   h1, h2

    */
    // Case 2 : data points are distributed equally, no values are lost, two hosts.
    metricValues.put(startTime + 4*seconds, 1.0);
    metricValues.put(startTime + 14*seconds, 2.0);
    metricValues.put(startTime + 24*seconds, 3.0);
    metricValues.put(startTime + 34*seconds, 4.0);
    metricValues.put(startTime + 44*seconds, 5.0);
    metricValues.put(startTime + 54*seconds, 6.0);

    timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setMetricValues(metricValues);

    metricValues = new TreeMap<>();
    metricValues.put(startTime + 5*seconds, 2.0);
    metricValues.put(startTime + 15*seconds, 4.0);
    metricValues.put(startTime + 25*seconds, 6.0);
    metricValues.put(startTime + 35*seconds, 8.0);
    metricValues.put(startTime + 45*seconds, 10.0);
    metricValues.put(startTime + 55*seconds, 12.0);
    TimelineMetric timelineMetric2 = new TimelineMetric("metric1", "host2", "app1", "instance1");
    timelineMetric2.setMetricValues(metricValues);

    timelineMetrics = new ArrayList<>();
    timelineMetrics.add(timelineMetric);
    timelineMetrics.add(timelineMetric2);
    timelineMetricsIgniteCache.putMetrics(timelineMetrics, metricMetadataManagerMock);
    aggregateMap = timelineMetricsIgniteCache.evictMetricAggregates(startTime, startTime + 120*seconds);

    Assert.assertEquals(aggregateMap.size(), 2);
    timelineClusterMetric = new TimelineClusterMetric(timelineMetric.getMetricName(),
      timelineMetric.getAppId(), timelineMetric.getInstanceId(), startTime + 30*seconds);

    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(6.0, aggregateMap.get(timelineClusterMetric).getSum());

    timelineClusterMetric.setTimestamp(startTime + 2*30*seconds);
    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(15.0, aggregateMap.get(timelineClusterMetric).getSum());

    metricValues.clear();
    timelineMetrics.clear();

    Assert.assertEquals(0d, timelineMetricsIgniteCache.getPointInTimeCacheMetrics().get("Cluster_KeySize"));
  }

  @Test
  public void updateAppAggregatesFromHostMetricTest() {
    //make sure hosts metrics are aggregated for appIds from "timeline.metrics.service.cluster.aggregator.appIds"

    long cacheSliceIntervalMillis = 30000L;

    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class))).andReturn(new byte[16]).once();
    expect(metricMetadataManagerMock.getHostedAppsCache()).andReturn(new HashMap<>()).anyTimes();
    replay(metricMetadataManagerMock);

    long startTime = getRoundedCheckPointTimeMillis(System.currentTimeMillis(), cacheSliceIntervalMillis);

    long seconds = 1000;

    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();
    List<TimelineMetric> timelineMetrics = new ArrayList<>();
    TimelineMetric timelineMetric;

    metricValues = new TreeMap<>();
    metricValues.put(startTime + 15*seconds, 1.0);
    metricValues.put(startTime + 55*seconds, 2.0);
    timelineMetric = new TimelineMetric("host_metric", "host1", HOST_APP_ID, "instance1");
    timelineMetric.setMetricValues(metricValues);
    timelineMetrics.add(timelineMetric);

    metricValues = new TreeMap<>();
    metricValues.put(startTime + 45*seconds, 3.0);
    metricValues.put(startTime + 85*seconds, 4.0);
    timelineMetric = new TimelineMetric("app_metric", "host1", "appIdForHostsAggr", "instance1");
    timelineMetric.setMetricValues(metricValues);
    timelineMetrics.add(timelineMetric);

    metricValues = new TreeMap<>();
    metricValues.put(startTime + 85*seconds, 5.0);
    timelineMetric = new TimelineMetric("host_metric", "host1", HOST_APP_ID, "instance1");
    timelineMetric.setMetricValues(metricValues);
    timelineMetrics.add(timelineMetric);

    metricValues = new TreeMap<>();
    metricValues.put(startTime + 85*seconds, 6.0);
    timelineMetric = new TimelineMetric("host_metric", "host2", HOST_APP_ID, "instance1");
    timelineMetric.setMetricValues(metricValues);
    timelineMetrics.add(timelineMetric);

    timelineMetricsIgniteCache.putMetrics(timelineMetrics, metricMetadataManagerMock);

    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMap = timelineMetricsIgniteCache.evictMetricAggregates(startTime, startTime + 120*seconds);
    Assert.assertEquals(aggregateMap.size(), 6);
    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(timelineMetric.getMetricName(),
        timelineMetric.getAppId(), timelineMetric.getInstanceId(), startTime + 90*seconds);

    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(11.0, aggregateMap.get(timelineClusterMetric).getSum());

    timelineClusterMetric = new TimelineClusterMetric("app_metric",
        "appIdForHostsAggr", "instance1", startTime + 90*seconds);
    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(4.0, aggregateMap.get(timelineClusterMetric).getSum());

    timelineClusterMetric = new TimelineClusterMetric("host_metric",
        "appIdForHostsAggr", "instance1", startTime + 90*seconds);
    Assert.assertTrue(aggregateMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(5.0, aggregateMap.get(timelineClusterMetric).getSum());

    Assert.assertEquals(0d, timelineMetricsIgniteCache.getPointInTimeCacheMetrics().get("Cluster_KeySize"));
  }
}
