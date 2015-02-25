/*
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

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink.COLLECTOR_HOST_PROPERTY;
import static org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink.MAX_METRIC_ROW_CACHE_SIZE;
import static org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink.METRICS_SEND_INTERVAL;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.verify;

public class HadoopTimelineMetricsSinkTest {

  @Test
  public void testPutMetrics() throws Exception {
    HadoopTimelineMetricsSink sink = new HadoopTimelineMetricsSink();

    SubsetConfiguration conf = createNiceMock(SubsetConfiguration.class);
    expect(conf.getString(eq("slave.host.name"))).andReturn("testhost").anyTimes();
    expect(conf.getParent()).andReturn(null).anyTimes();
    expect(conf.getPrefix()).andReturn("service").anyTimes();
    expect(conf.getString(eq(COLLECTOR_HOST_PROPERTY))).andReturn("localhost:63188").anyTimes();
    expect(conf.getString(eq("serviceName-prefix"), eq(""))).andReturn("").anyTimes();

    expect(conf.getInt(eq(MAX_METRIC_ROW_CACHE_SIZE), anyInt())).andReturn(10).anyTimes();
    expect(conf.getInt(eq(METRICS_SEND_INTERVAL), anyInt())).andReturn(1000).anyTimes();

    conf.setListDelimiter(eq(','));
    expectLastCall().anyTimes();

    expect(conf.getKeys()).andReturn(new Iterator() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Object next() {
        return null;
      }

      @Override
      public void remove() {

      }
    }).once();


    HttpClient httpClient = createNiceMock(HttpClient.class);

    expect(httpClient.executeMethod(anyObject(PostMethod.class))).andReturn(200).once(); //metrics send only once due to caching

    AbstractMetric metric = createNiceMock(AbstractMetric.class);
    expect(metric.name()).andReturn("metricName").anyTimes();
    expect(metric.value()).andReturn(9.5687).anyTimes();
    //TODO currently only numeric metrics are supported

    MetricsRecord record = createNiceMock(MetricsRecord.class);
    expect(record.name()).andReturn("testName").anyTimes();
    expect(record.context()).andReturn("testContext").anyTimes();
    expect(record.timestamp()).andAnswer(new IAnswer<Long>() {
      @Override
      public Long answer() throws Throwable {
        return System.currentTimeMillis();
      }
    }).anyTimes();

    expect(record.metrics()).andReturn(Arrays.asList(metric)).anyTimes();


    replay(conf, httpClient, record, metric);

    sink.setHttpClient(httpClient);
    sink.init(conf);

    sink.putMetrics(record);

    Thread.sleep(1500L);

    sink.putMetrics(record);

    verify(conf, httpClient, record, metric);
  }

  @Test
  public void testDuplicateTimeSeriesNotSaved() throws Exception {
    HadoopTimelineMetricsSink sink =
      createMockBuilder(HadoopTimelineMetricsSink.class)
        .withConstructor().addMockedMethod("appendPrefix")
        .addMockedMethod("emitMetrics").createNiceMock();

    SubsetConfiguration conf = createNiceMock(SubsetConfiguration.class);
    expect(conf.getString(eq("slave.host.name"))).andReturn("testhost").anyTimes();
    expect(conf.getParent()).andReturn(null).anyTimes();
    expect(conf.getPrefix()).andReturn("service").anyTimes();
    expect(conf.getString(eq(COLLECTOR_HOST_PROPERTY))).andReturn("localhost:63188").anyTimes();
    expect(conf.getString(eq("serviceName-prefix"), eq(""))).andReturn("").anyTimes();

    expect(conf.getInt(eq(MAX_METRIC_ROW_CACHE_SIZE), anyInt())).andReturn(10).anyTimes();
    // Return eviction time smaller than time diff for first 3 entries
    // Third entry will result in eviction
    expect(conf.getInt(eq(METRICS_SEND_INTERVAL), anyInt())).andReturn(10).anyTimes();

    conf.setListDelimiter(eq(','));
    expectLastCall().anyTimes();

    expect(conf.getKeys()).andReturn(new Iterator() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Object next() {
        return null;
      }

      @Override
      public void remove() {

      }
    }).once();

    AbstractMetric metric = createNiceMock(AbstractMetric.class);
    expect(metric.name()).andReturn("metricName").anyTimes();
    expect(metric.value()).andReturn(1.0).once();
    expect(metric.value()).andReturn(2.0).once();
    expect(metric.value()).andReturn(3.0).once();
    expect(metric.value()).andReturn(4.0).once();
    expect(metric.value()).andReturn(5.0).once();
    expect(metric.value()).andReturn(6.0).once();

    MetricsRecord record = createNiceMock(MetricsRecord.class);
    expect(record.name()).andReturn("testName").anyTimes();
    expect(record.context()).andReturn("testContext").anyTimes();

    sink.appendPrefix(eq(record), (StringBuilder) anyObject());
    expectLastCall().anyTimes().andStubAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        return null;
      }
    });

    final Long now = System.currentTimeMillis();
    // TODO: Current implementation of cache needs > 1 elements to evict any
    expect(record.timestamp()).andReturn(now).times(2);
    expect(record.timestamp()).andReturn(now + 100l).times(2);
    expect(record.timestamp()).andReturn(now + 200l).once();
    expect(record.timestamp()).andReturn(now + 300l).once();

    expect(record.metrics()).andReturn(Arrays.asList(metric)).anyTimes();

    final List<TimelineMetrics> capturedMetrics = new ArrayList<TimelineMetrics>();
    sink.emitMetrics((TimelineMetrics) anyObject());
    expectLastCall().andStubAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        capturedMetrics.add((TimelineMetrics) EasyMock.getCurrentArguments()[0]);
        return null;
      }
    });

    replay(conf, sink, record, metric);

    sink.init(conf);

    // time = t1
    sink.putMetrics(record);
    // time = t1
    sink.putMetrics(record);
    // time = t2
    sink.putMetrics(record);
    // Evict
    // time = t2
    sink.putMetrics(record);
    // time = t3
    sink.putMetrics(record);
    // time = t4
    sink.putMetrics(record);

    verify(conf, sink, record, metric);

    Assert.assertEquals(2, capturedMetrics.size());
    Iterator<TimelineMetrics> metricsIterator = capturedMetrics.iterator();

    // t1, t2
    TimelineMetric timelineMetric1 = metricsIterator.next().getMetrics().get(0);
    Assert.assertEquals(2, timelineMetric1.getMetricValues().size());
    Iterator<Long> timestamps = timelineMetric1.getMetricValues().keySet().iterator();
    Assert.assertEquals(now, timestamps.next());
    Assert.assertEquals(new Long(now + 100l), timestamps.next());
    Iterator<Double> values = timelineMetric1.getMetricValues().values().iterator();
    Assert.assertEquals(new Double(1.0), values.next());
    Assert.assertEquals(new Double(3.0), values.next());
    // t3, t4
    TimelineMetric timelineMetric2 = metricsIterator.next().getMetrics().get(0);
    Assert.assertEquals(2, timelineMetric2.getMetricValues().size());
    timestamps = timelineMetric2.getMetricValues().keySet().iterator();
    Assert.assertEquals(new Long(now + 200l), timestamps.next());
    Assert.assertEquals(new Long(now + 300l), timestamps.next());
    values = timelineMetric2.getMetricValues().values().iterator();
    Assert.assertEquals(new Double(5.0), values.next());
    Assert.assertEquals(new Double(6.0), values.next());
  }
}