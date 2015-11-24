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

package org.apache.hadoop.metrics2.sink.storm;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.junit.Test;

import backtype.storm.metric.api.IMetricsConsumer;

public class StormTimelineMetricsSinkTest {
  @Test
  public void testNonNumericMetricMetricExclusion() throws InterruptedException, IOException {
    StormTimelineMetricsSink stormTimelineMetricsSink = new StormTimelineMetricsSink();
    TimelineMetricsCache timelineMetricsCache = createNiceMock(TimelineMetricsCache.class);
    stormTimelineMetricsSink.setMetricsCache(timelineMetricsCache);
    replay(timelineMetricsCache);
    stormTimelineMetricsSink.handleDataPoints(
        new IMetricsConsumer.TaskInfo("localhost", 1234, "testComponent", 42, 20000L, 60),
        Collections.singleton(new IMetricsConsumer.DataPoint("key1", "value1")));
    verify(timelineMetricsCache);
  }

  @Test
  public void testNumericMetricMetricSubmission() throws InterruptedException, IOException {
    StormTimelineMetricsSink stormTimelineMetricsSink = new StormTimelineMetricsSink();
    TimelineMetricsCache timelineMetricsCache = createNiceMock(TimelineMetricsCache.class);
    expect(timelineMetricsCache.getTimelineMetric("key1"))
        .andReturn(new TimelineMetric()).once();
    timelineMetricsCache.putTimelineMetric(anyObject(TimelineMetric.class));
    expectLastCall().once();
    stormTimelineMetricsSink.setMetricsCache(timelineMetricsCache);
    replay(timelineMetricsCache);
    stormTimelineMetricsSink.handleDataPoints(
        new IMetricsConsumer.TaskInfo("localhost", 1234, "testComponent", 42, 20000L, 60),
        Collections.singleton(new IMetricsConsumer.DataPoint("key1", 42)));
    verify(timelineMetricsCache);
  }
}
