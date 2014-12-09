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
import org.apache.hadoop.metrics2.sink.timeline.base.AbstractTimelineMetricsSink;
import org.easymock.IAnswer;

import java.util.Arrays;
import java.util.Iterator;

import static org.apache.hadoop.metrics2.sink.timeline.base.AbstractTimelineMetricsSink.*;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class HadoopTimelineMetricsSinkTest {

  @org.junit.Test
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
}