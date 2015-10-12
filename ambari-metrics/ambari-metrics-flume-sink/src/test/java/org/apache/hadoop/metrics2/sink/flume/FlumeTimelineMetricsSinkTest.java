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

package org.apache.hadoop.metrics2.sink.flume;

import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.Collections;

import org.apache.commons.httpclient.HttpClient;
import org.apache.flume.instrumentation.util.JMXPollUtil;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JMXPollUtil.class)
public class FlumeTimelineMetricsSinkTest {
  @Test
  public void testNonNumericMetricMetricExclusion() throws InterruptedException {
    FlumeTimelineMetricsSink flumeTimelineMetricsSink = new FlumeTimelineMetricsSink();
    FlumeTimelineMetricsSink.TimelineMetricsCollector collector =
      flumeTimelineMetricsSink.new TimelineMetricsCollector();
    mockStatic(JMXPollUtil.class);
    EasyMock.expect(JMXPollUtil.getAllMBeans()).andReturn(
        Collections.singletonMap("component1", Collections.singletonMap("key1", "value1"))).once();
    replay(JMXPollUtil.class);
    collector.run();
    verifyAll();
  }

  @Test
  public void testNumericMetricSubmission() throws InterruptedException {
    FlumeTimelineMetricsSink flumeTimelineMetricsSink = new FlumeTimelineMetricsSink();
    FlumeTimelineMetricsSink.TimelineMetricsCollector collector =
      flumeTimelineMetricsSink.new TimelineMetricsCollector();
    mockStatic(JMXPollUtil.class);
    EasyMock.expect(JMXPollUtil.getAllMBeans()).andReturn(
        Collections.singletonMap("component1", Collections.singletonMap("key1", "42"))).once();
    replay(JMXPollUtil.class);
    collector.run();
    verifyAll();
  }

  private TimelineMetricsCache getTimelineMetricsCache(FlumeTimelineMetricsSink flumeTimelineMetricsSink) {
    TimelineMetricsCache timelineMetricsCache = EasyMock.createNiceMock(TimelineMetricsCache.class);
    flumeTimelineMetricsSink.setMetricsCaches(Collections.singletonMap("SINK",timelineMetricsCache));
    EasyMock.expect(timelineMetricsCache.getTimelineMetric("key1"))
        .andReturn(new TimelineMetric()).once();
    timelineMetricsCache.putTimelineMetric(EasyMock.anyObject(TimelineMetric.class));
    EasyMock.expectLastCall().once();
    return timelineMetricsCache;
  }

  @Test
  public void testMonitorRestart() throws InterruptedException {
    FlumeTimelineMetricsSink flumeTimelineMetricsSink = new FlumeTimelineMetricsSink();
    TimelineMetricsCache timelineMetricsCache = getTimelineMetricsCache(flumeTimelineMetricsSink);
    flumeTimelineMetricsSink.setPollFrequency(1);
    HttpClient httpClient = EasyMock.createNiceMock(HttpClient.class);
    flumeTimelineMetricsSink.setHttpClient(httpClient);
    mockStatic(JMXPollUtil.class);
    EasyMock.expect(JMXPollUtil.getAllMBeans()).andReturn(
        Collections.singletonMap("component1", Collections.singletonMap("key1", "42"))).once();
    flumeTimelineMetricsSink.start();
    flumeTimelineMetricsSink.stop();
    replay(JMXPollUtil.class, timelineMetricsCache, httpClient);
    flumeTimelineMetricsSink.start();
    Thread.sleep(5);
    flumeTimelineMetricsSink.stop();
    verifyAll();
  }

  @Test
  public void testMetricsRetrievalExceptionTolerance() throws InterruptedException {
    FlumeTimelineMetricsSink flumeTimelineMetricsSink = new FlumeTimelineMetricsSink();
    FlumeTimelineMetricsSink.TimelineMetricsCollector collector =
      flumeTimelineMetricsSink.new TimelineMetricsCollector();
    mockStatic(JMXPollUtil.class);
    EasyMock.expect(JMXPollUtil.getAllMBeans()).
        andThrow(new RuntimeException("Failed to retrieve Flume Properties")).once();
    replay(JMXPollUtil.class);
    collector.run();
    verifyAll();
  }
}
