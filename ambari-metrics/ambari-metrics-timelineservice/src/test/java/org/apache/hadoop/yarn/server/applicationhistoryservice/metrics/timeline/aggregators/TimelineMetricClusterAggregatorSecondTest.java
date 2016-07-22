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
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND;

public class TimelineMetricClusterAggregatorSecondTest {

  @Test
  public void testClusterSecondAggregatorWithInterpolation() {

    long aggregatorInterval = 120000l;
    long sliceInterval = 30000l;
    long metricInterval = 10000l;

    Configuration configuration = new Configuration();
    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);

    TimelineMetricClusterAggregatorSecond secondAggregator = new TimelineMetricClusterAggregatorSecond(
      METRIC_AGGREGATE_SECOND, metricMetadataManagerMock, null,
      configuration, null, aggregatorInterval, 2, "false", "", "",
      aggregatorInterval, sliceInterval, null);

    secondAggregator.timeSliceIntervalMillis = sliceInterval;
    long roundedEndTime = AbstractTimelineAggregator.getRoundedAggregateTimeMillis(aggregatorInterval);
    long roundedStartTime = roundedEndTime - aggregatorInterval;
    List<Long[]> timeSlices = secondAggregator.getTimeSlices(roundedStartTime ,
      roundedEndTime);

    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();

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

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(counterMetric, timeSlices);

    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(counterMetric.getMetricName(), counterMetric.getAppId(),
      counterMetric.getInstanceId(), 0l, null);

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

    timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(metric, timeSlices);

    timelineClusterMetric = new TimelineClusterMetric(metric.getMetricName(), metric.getAppId(),
      metric.getInstanceId(), 0l, null);

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
    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);

    TimelineMetricClusterAggregatorSecond secondAggregator = new TimelineMetricClusterAggregatorSecond(
      METRIC_AGGREGATE_SECOND, metricMetadataManagerMock, null, configuration, null,
      aggregatorInterval, 2, "false", "", "", aggregatorInterval, sliceInterval, null
    );

    long startTime = AbstractTimelineAggregator.getRoundedCheckPointTimeMillis(System.currentTimeMillis(),aggregatorInterval);
    List<Long[]> timeslices = secondAggregator.getTimeSlices(startTime, startTime + aggregatorInterval);

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
      timelineMetric.getAppId(), timelineMetric.getInstanceId(), startTime + 30*seconds, timelineMetric.getType());

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
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

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
    secondAggregator.processAggregateClusterMetrics(aggregateClusterMetrics, timelineMetric, timeslices);

    Assert.assertEquals(aggregateClusterMetrics.size(), 4);
    timelineClusterMetric.setTimestamp(startTime + 60*seconds);
    Assert.assertTrue(aggregateClusterMetrics.containsKey(timelineClusterMetric));
    Assert.assertEquals(aggregateClusterMetrics.get(timelineClusterMetric).getSum(), 2.5);

    metricValues.clear();
    aggregateClusterMetrics.clear();

  }

}
