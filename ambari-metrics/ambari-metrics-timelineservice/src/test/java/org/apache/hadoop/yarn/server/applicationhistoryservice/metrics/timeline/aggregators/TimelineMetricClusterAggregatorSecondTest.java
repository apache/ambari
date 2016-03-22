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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TimelineMetricClusterAggregatorSecondTest {

  @Test
  public void testClusterSecondAggregatorWithInterpolation() {

    long aggregatorInterval = 120000l;
    long sliceInterval = 30000l;
    long metricInterval = 10000l;

    Configuration configuration = new Configuration();
    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);

    TimelineMetricClusterAggregatorSecond secondAggregator = new TimelineMetricClusterAggregatorSecond(
      "TimelineClusterAggregatorSecond", metricMetadataManagerMock, null, configuration, null,
      aggregatorInterval, 2, "false", "", "", aggregatorInterval, sliceInterval
    );

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

    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName("TestMetric");
    metric.setHostName("TestHost");
    metric.setAppId("TestAppId");
    metric.setMetricValues(metricValues);

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(metric, timeSlices);

    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(metric.getMetricName(), metric.getAppId(),
      metric.getInstanceId(), 0l, null);

    timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 4.5);

    timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 7.5);

  }

}
