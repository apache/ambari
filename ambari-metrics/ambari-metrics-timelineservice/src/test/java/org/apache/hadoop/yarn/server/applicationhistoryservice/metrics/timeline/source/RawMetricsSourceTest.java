/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source.InternalSourceProvider.SOURCE_NAME.RAW_METRICS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.sink.ExternalMetricsSink;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimelineMetricConfiguration.class)
public class RawMetricsSourceTest {

  @Before
  public void setupConf() throws Exception {
    TimelineMetricConfiguration conf = new TimelineMetricConfiguration(new
      Configuration(), new Configuration());
    mockStatic(TimelineMetricConfiguration.class);
    expect(TimelineMetricConfiguration.getInstance()).andReturn(conf).anyTimes();
    replayAll();
  }

  @Test
  public void testRawMetricsSourcedAtFlushInterval() throws Exception {
    InternalSourceProvider internalSourceProvider = new DefaultInternalMetricsSourceProvider();
    ExternalMetricsSink rawMetricsSink = createNiceMock(ExternalMetricsSink.class);
    expect(rawMetricsSink.getFlushSeconds()).andReturn(1);
    expect(rawMetricsSink.getSinkTimeOutSeconds()).andReturn(1);
    Capture<Collection<TimelineMetrics>> metricsCapture = new Capture<>();
    rawMetricsSink.sinkMetricData(capture(metricsCapture));
    expectLastCall();
    replay(rawMetricsSink);

    InternalMetricsSource rawMetricsSource = internalSourceProvider.getInternalMetricsSource(RAW_METRICS, 1, rawMetricsSink);
    TimelineMetrics timelineMetrics = new TimelineMetrics();

    final long now = System.currentTimeMillis();
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("m1");
    metric1.setAppId("a1");
    metric1.setInstanceId("i1");
    metric1.setHostName("h1");
    metric1.setStartTime(now - 200);
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
    }});
    timelineMetrics.getMetrics().add(metric1);

    rawMetricsSource.publishTimelineMetrics(Collections.singletonList(timelineMetrics));

    verify(rawMetricsSink);
  }

  @Test(timeout = 10000)
  public void testRawMetricsCachedAndSourced() throws Exception {
    ExternalMetricsSink rawMetricsSink = createNiceMock(ExternalMetricsSink.class);
    expect(rawMetricsSink.getFlushSeconds()).andReturn(2).anyTimes();
    expect(rawMetricsSink.getSinkTimeOutSeconds()).andReturn(1).anyTimes();

    class CaptureOnce<T> extends Capture<T> {
      @Override
      public void setValue(T value) {
        if (!hasCaptured()) {
          super.setValue(value);
        }
      }
    }
    Capture<Collection<TimelineMetrics>> metricsCapture = new CaptureOnce<>();

    rawMetricsSink.sinkMetricData(capture(metricsCapture));
    expectLastCall();
    replay(rawMetricsSink);

    InternalSourceProvider internalSourceProvider = new DefaultInternalMetricsSourceProvider();
    InternalMetricsSource rawMetricsSource = internalSourceProvider.getInternalMetricsSource(RAW_METRICS, 1, rawMetricsSink);
    TimelineMetrics timelineMetrics = new TimelineMetrics();

    final long now = System.currentTimeMillis();
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("m1");
    metric1.setAppId("a1");
    metric1.setInstanceId("i1");
    metric1.setHostName("h1");
    metric1.setStartTime(now - 200);
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(now - 100, 1.0);
      put(now - 200, 2.0);
    }});
    timelineMetrics.getMetrics().add(metric1);

    rawMetricsSource.publishTimelineMetrics(Collections.singletonList(timelineMetrics));

    // Wait on eviction
    Thread.sleep(5000);

    verify(rawMetricsSink);

    Assert.assertTrue(metricsCapture.hasCaptured());
    Assert.assertTrue(metricsCapture.getValue().iterator().next().getMetrics().iterator().next().equals(metric1));
  }

}
