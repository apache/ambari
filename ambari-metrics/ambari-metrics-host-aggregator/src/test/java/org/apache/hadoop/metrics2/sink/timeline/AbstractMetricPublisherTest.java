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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolder;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolderTest;
import org.junit.Test;

import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class AbstractMetricPublisherTest {
    @Test
    public void testProcessAndPublishMetrics() throws Exception {
        AbstractMetricPublisher publisherMock =
                createMockBuilder(RawMetricsPublisher.class)
                        .withConstructor(TimelineMetricsHolder.getInstance(), new Configuration(), 60)
                        .addMockedMethod("processMetrics")
                        .addMockedMethod("getCollectorUri")
                        .addMockedMethod("emitMetricsJson")
                        .addMockedMethod("getCurrentCollectorHost").createStrictMock();

        TimelineMetricsHolder.getInstance().putMetricsForRawPublishing(TimelineMetricsHolderTest.getTimelineMetricsWithAppID("raw"));
        expect(publisherMock.getCurrentCollectorHost()).andReturn("collectorhost").once();
        expect(publisherMock.getCollectorUri(anyString())).andReturn("https://collectorhost:11/metrics").once();
        expect(publisherMock.processMetrics(anyObject(Map.class))).andReturn("{metrics}").once();
        expect(publisherMock.emitMetricsJson("https://collectorhost:11/metrics", "{metrics}")).andReturn(true).once();

        replay(publisherMock);

        publisherMock.processAndPublishMetrics(TimelineMetricsHolder.getInstance().extractMetricsForRawPublishing());

        verify(publisherMock);
    }

    @Test
    public void testRunAndStop() throws Exception {
        AbstractMetricPublisher publisherMock = createMockBuilder(RawMetricsPublisher.class)
                .withConstructor(TimelineMetricsHolder.getInstance(), new Configuration(), 1)
                .addMockedMethod("processAndPublishMetrics").createStrictMock();
        publisherMock.processAndPublishMetrics(anyObject(Map.class));
        expectLastCall().times(1);


        Thread t = createMockBuilder(Thread.class)
                .withConstructor(publisherMock)
                .addMockedMethod("isInterrupted").createStrictMock();
        expect(t.isInterrupted()).andReturn(false).once();
        expect(t.isInterrupted()).andReturn(true).once();

        replay(publisherMock, t);

        t.start();

        Thread.sleep(2222);

        verify(publisherMock, t);
    }
}
