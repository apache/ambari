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


import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getRoundedAggregateTimeMillis;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getRoundedCheckPointTimeMillis;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getTimeSlices;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.sliceFromTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

 import junit.framework.Assert;

public class TimelineMetricClusterAggregatorSecondTest {

  @Test
  public void testClusterSecondAggregatorWithInterpolation() {

    long aggregatorInterval = 120000l;
    long sliceInterval = 30000l;
    long metricInterval = 10000l;

    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class))).andReturn(new byte[16]).once();
    replay(metricMetadataManagerMock);

    long roundedEndTime = getRoundedAggregateTimeMillis(aggregatorInterval);
    long roundedStartTime = roundedEndTime - aggregatorInterval;
    List<Long[]> timeSlices = getTimeSlices(roundedStartTime ,
      roundedEndTime, sliceInterval);

    TreeMap<Long, Double> metricValues = new TreeMap<>();

    long startTime = roundedEndTime - aggregatorInterval;

    for (int i=1; startTime < roundedEndTime; i++) {
      startTime += metricInterval;
      if (i%6 == 1 || i%6 == 2) {
        metricValues.put(startTime, (double)i);
      }
    }

    TimelineMetric counterMetric = new TimelineMetric();
    counterMetric.setMetricName("TestMetric");
    counterMetric.setHostName("TestHost");
    counterMetric.setAppId("TestAppId");
    counterMetric.setMetricValues(metricValues);
    counterMetric.setType("COUNTER");

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap = sliceFromTimelineMetric(counterMetric, timeSlices, true);

    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(counterMetric.getMetricName(), counterMetric.getAppId(),
      counterMetric.getInstanceId(), 0l);

    timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 6.0);

    timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 12.0);

    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName("TestMetric");
    metric.setHostName("TestHost");
    metric.setAppId("TestAppId");
    metric.setMetricValues(metricValues);

    timelineClusterMetricMap = sliceFromTimelineMetric(metric, timeSlices, true);

    timelineClusterMetric = new TimelineClusterMetric(metric.getMetricName(), metric.getAppId(),
      metric.getInstanceId(), 0l);

    timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 4.5);

    timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 7.5);
  }

  @Test
  public void testShouldAggregateProperly() {

    long aggregatorInterval = 120000l;
    long sliceInterval = 30000l;

    Configuration configuration = new Configuration();
    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);

    expect(metricMetadataManagerMock.getMetadataCacheValue((TimelineMetricMetadataKey) anyObject()))
      .andReturn(null).anyTimes();
    replay(metricMetadataManagerMock);

    TimelineMetricClusterAggregatorSecond secondAggregator = new TimelineMetricClusterAggregatorSecond(
      METRIC_AGGREGATE_SECOND, metricMetadataManagerMock, null, configuration, null,
      aggregatorInterval, 2, "false", "", "", aggregatorInterval, sliceInterval, null
    );

    long startTime = getRoundedCheckPointTimeMillis(System.currentTimeMillis(),aggregatorInterval);
    List<Long[]> timeslices = getTimeSlices(startTime, startTime + aggregatorInterval, sliceInterval);

    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics = new HashMap<>();
    long seconds = 1000;
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setType("GUAGE");
    timelineMetric.setStartTime(startTime);

    /*

    0      +30s    +60s    +90s    +120s   +150s   +180s
    |       |       |       |       |       |       |
     (1)        (2)      (3)    (4)   (5)     (6)

    */
    // Case 1 : Points present in all the required timeslices.
    // Life is good! Ignore (5) and (6).

    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();
    metricValues.put(startTime + 15*seconds, 1.0);
    metricValues.put(startTime + 45*seconds, 2.0);
    metricValues.put(startTime + 75*seconds, 3.0);
    metricValues.put(startTime + 105*seconds, 4.0);
    metricValues.put(startTime + 135*seconds, 5.0);
    metricValues.put(startTime + 165*seconds, 6.0);

    timelineMetric.setMetricValues(metricValues);
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(timelineMetric.getMetricName(),
      timelineMetric.getAppId(), timelineMetric.getInstanceId(), startTime + 30*seconds);

    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 1.0);

    timelineClusterMetric.setTimestamp(startTime + 4*30*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(),4.0);

    metricValues.clear();
    aggregateClusterMetrics.clear();

    /*

    0      +30s    +60s    +90s    +120s   +150s   +180s
    |       |       |       |       |       |       |
     (1)              (3)      (4)    (5)     (6)

    */
    // Case 2 : Some "middle" point missing in the required timeslices.
    // Interpolate the middle point. Ignore (5) and (6).
    metricValues.put(startTime + 15*seconds, 1.0);
    metricValues.put(startTime + 75*seconds, 3.0);
    metricValues.put(startTime + 105*seconds, 4.0);
    metricValues.put(startTime + 135*seconds, 5.0);
    metricValues.put(startTime + 165*seconds, 6.0);

    timelineMetric.setMetricValues(metricValues);
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 60*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 2.0);

    metricValues.clear();
    aggregateClusterMetrics.clear();


    /*

    0      +30s    +60s    +90s    +120s   +150s   +180s
    |       |       |       |       |       |       |
     (1)        (2)    (3)              (5)     (6)

    */
    // Case 3 : "end" point missing in the required timeslices.
    // Use all points to get missing point if COUNTER. Else use just (3). Ignore (6).
    metricValues.put(startTime + 15*seconds, 1.0);
    metricValues.put(startTime + 45*seconds, 2.0);
    metricValues.put(startTime + 75*seconds, 3.0);
    metricValues.put(startTime + 135*seconds, 5.0);
    metricValues.put(startTime + 165*seconds, 6.0);

    timelineMetric.setMetricValues(metricValues);
    timelineMetric.setType("GUAGE");
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 120*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 3.0);

    aggregateClusterMetrics.clear();

    timelineMetric.setType("COUNTER");
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 120*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 4.5);

    metricValues.clear();
    aggregateClusterMetrics.clear();

    /*

    0      +30s    +60s    +90s    +120s   +150s   +180s
    |       |       |       |       |       |       |
              (2)      (3)     (4)     (5)     (6)

    */
    // Case 4 : "start" point missing in the required timeslices.
    // Interpolate with only (2) to get missing point if GUAGE metric. Else use all points for COUNTER.

    metricValues.put(startTime + 45*seconds, 2.0);
    metricValues.put(startTime + 75*seconds, 3.0);
    metricValues.put(startTime + 105*seconds, 4.0);
    metricValues.put(startTime + 135*seconds, 5.0);
    metricValues.put(startTime + 165*seconds, 6.0);

    timelineMetric.setMetricValues(metricValues);
    timelineMetric.setType("GUAGE");
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 30*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 2.0);

    aggregateClusterMetrics.clear();

    timelineMetric.setType("COUNTER");
    int liveHosts = secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(liveHosts, 1);
    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 30*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 1.5);

    metricValues.clear();
    aggregateClusterMetrics.clear();

    /*

    0      +30s    +60s    +90s    +120s   +150s   +180s
    |       |       |       |       |       |       |
                                        (5)     (6)

    */
    // Case 5 : Well, we have nothing in the 2 min window.
    // Use (5) to paint the 2 min window as (5).

    metricValues.put(startTime + 135*seconds, 5.0);
    metricValues.put(startTime + 165*seconds, 6.0);

    timelineMetric.setMetricValues(metricValues);
    timelineMetric.setType("GUAGE");
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 30*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 5.0);

    aggregateClusterMetrics.clear();

    timelineMetric.setType("COUNTER");
    liveHosts = secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(liveHosts, 1);
    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 60*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 2.5);

    metricValues.clear();
    aggregateClusterMetrics.clear();

  }

  @Test
  public void testLiveHostCounterMetrics() throws Exception {
    long aggregatorInterval = 120000;
    long sliceInterval = 30000;

    Configuration configuration = new Configuration();
    TimelineMetricMetadataManager metricMetadataManagerMock = createNiceMock(TimelineMetricMetadataManager.class);

    expect(metricMetadataManagerMock.getMetadataCacheValue((TimelineMetricMetadataKey) anyObject())).andReturn(null).anyTimes();

    /*
    m1-h1-a1
    m2-h1-a1
    m2-h1-a2
    m2-h2-a1
    m2-h2-a2
    m2-h3-a2

    So live_hosts : a1 = 2
       live_hosts : a2 = 3
     */

    TimelineMetric metric1  = new TimelineMetric("m1", "h1", "a1", null);
    TimelineMetric metric2  = new TimelineMetric("m2", "h1", "a1", null);
    TimelineMetric metric3  = new TimelineMetric("m2", "h1", "a2", null);
    TimelineMetric metric4  = new TimelineMetric("m2", "h2", "a1", null);
    TimelineMetric metric5  = new TimelineMetric("m2", "h2", "a2", null);
    TimelineMetric metric6  = new TimelineMetric("m2", "h3", "a2", null);

    expect(metricMetadataManagerMock.getMetricFromUuid((byte[]) anyObject())).
      andReturn(metric1).andReturn(metric2).andReturn(metric3).
      andReturn(metric4).andReturn(metric5).andReturn(metric6);
    replay(metricMetadataManagerMock);

    TimelineMetricClusterAggregatorSecond secondAggregator = new TimelineMetricClusterAggregatorSecond(
      METRIC_AGGREGATE_SECOND, metricMetadataManagerMock, null, configuration, null,
      aggregatorInterval, 2, "false", "", "", aggregatorInterval,
      sliceInterval, null);

    long now = System.currentTimeMillis();
    long startTime = now - 120000;
    long seconds = 1000;
    List<Long[]> slices = getTimeSlices(startTime, now, sliceInterval);
    ResultSet rs = createNiceMock(ResultSet.class);

    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(startTime + 15 * seconds, 1.0);
    metricValues.put(startTime + 45 * seconds, 2.0);
    metricValues.put(startTime + 75 * seconds, 3.0);
    metricValues.put(startTime + 105 * seconds, 4.0);

    expect(rs.next()).andReturn(true).times(6);
    expect(rs.next()).andReturn(false);

    expect(rs.getLong("SERVER_TIME")).andReturn(now - 150000).times(6);
    expect(rs.getLong("START_TIME")).andReturn(now - 150000).times(6);

    ObjectMapper mapper = new ObjectMapper();
    expect(rs.getString("METRICS")).andReturn(mapper.writeValueAsString(metricValues)).times(6);

    replay(rs);

    Map<TimelineClusterMetric, MetricClusterAggregate> aggregates = secondAggregator.aggregateMetricsFromResultSet(rs, slices);

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
    Assert.assertEquals(3d, a2.getSum());
  }
}
