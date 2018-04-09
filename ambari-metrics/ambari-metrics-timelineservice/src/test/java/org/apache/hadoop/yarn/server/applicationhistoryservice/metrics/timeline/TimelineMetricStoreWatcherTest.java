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

import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ExitUtil.class)
public class TimelineMetricStoreWatcherTest {

  @Test
  public void testRunPositive() throws Exception {
    TimelineMetricStore metricStore = createNiceMock(TimelineMetricStore.class);

    expect(metricStore.putMetrics(anyObject(TimelineMetrics.class)))
      .andReturn(new TimelinePutResponse());

    // metric found
    expect(metricStore.getTimelineMetrics(EasyMock.<List<String>>anyObject(),
      EasyMock.<List<String>>anyObject(), anyObject(String.class),
      anyObject(String.class), anyObject(Long.class), anyObject(Long.class),
      eq(Precision.SECONDS), eq(1), eq(true), anyObject(TopNConfig.class), anyString()))
      .andReturn(null).anyTimes();

    mockStatic(ExitUtil.class);

    replay(metricStore);

    TimelineMetricStoreWatcher timelineMetricStoreWatcher =
      new TimelineMetricStoreWatcher(metricStore, TimelineMetricConfiguration.getInstance());
    timelineMetricStoreWatcher.run();
    timelineMetricStoreWatcher.run();
    timelineMetricStoreWatcher.run();
    verify(metricStore);

  }

  @Test
  public void testRunNegative() throws Exception {
    TimelineMetricStore metricStore = createNiceMock(TimelineMetricStore.class);

    expect(metricStore.putMetrics(anyObject(TimelineMetrics.class)))
      .andReturn(new TimelinePutResponse());

    // no metrics found
    expect(metricStore.getTimelineMetrics(EasyMock.<List<String>>anyObject(),
      EasyMock.<List<String>>anyObject(), anyObject(String.class),
      anyObject(String.class), anyObject(Long.class), anyObject(Long.class),
      eq(Precision.SECONDS), eq(1), eq(true), anyObject(TopNConfig.class), anyString()))
      .andReturn(null).anyTimes();

    String msg = "Error getting metrics from TimelineMetricStore. " +
      "Shutting down by TimelineMetricStoreWatcher.";
    mockStatic(ExitUtil.class);
    ExitUtil.terminate(-1, msg);
    expectLastCall().anyTimes();

    replayAll();

    TimelineMetricStoreWatcher timelineMetricStoreWatcher =
      new TimelineMetricStoreWatcher(metricStore, TimelineMetricConfiguration.getInstance());
    timelineMetricStoreWatcher.run();
    timelineMetricStoreWatcher.run();
    timelineMetricStoreWatcher.run();

    verifyAll();

  }

}
