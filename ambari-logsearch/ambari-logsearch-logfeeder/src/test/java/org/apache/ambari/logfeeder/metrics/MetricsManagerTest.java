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

package org.apache.ambari.logfeeder.metrics;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.Before;
import org.junit.Test;

public class MetricsManagerTest {

  private MetricsManager manager;
  private LogFeederAMSClient mockClient;
  private Capture<TimelineMetrics> capture;
  
  @Before
  public void init() throws Exception {
    manager = new MetricsManager();

    mockClient = strictMock(LogFeederAMSClient.class);
    Field f = MetricsManager.class.getDeclaredField("amsClient");
    f.setAccessible(true);
    f.set(manager, mockClient);

    EasyMock.expect(mockClient.getCollectorUri(null)).andReturn("null://null:null/null").anyTimes();
    capture = EasyMock.newCapture(CaptureType.FIRST);
    mockClient.emitMetrics(EasyMock.capture(capture));
    EasyMock.expectLastCall().andReturn(true).once();

    replay(mockClient);
    manager.setAmsClient(mockClient);
    manager.init();
  }
  
  @Test
  public void testMetricManager_pointInTime() throws Exception {
    MetricData metricCount1 = new MetricData("metric1", true);
    metricCount1.value = 123;
    metricCount1.prevPublishValue = 0;
    metricCount1.publishCount = 0;
    
    manager.useMetrics(Arrays.asList(metricCount1));
    
    verify(mockClient);
    
    TimelineMetrics metrics = capture.getValue();
    List<TimelineMetric> metricList = metrics.getMetrics();
    assertEquals(metricList.size(), 1);
    
    TimelineMetric metric = metricList.get(0);
    assertEquals(metric.getAppId(), "logfeeder");
    assertEquals(metric.getMetricName(), "metric1");
    assertEquals(metric.getType(), "Long");
    
    TreeMap<Long, Double> values = metric.getMetricValues();
    assertEquals(values.size(), 1);
    assertEquals(values.firstEntry().getValue(), Double.valueOf(123.0));
  }
  
  @Test
  public void testMetricManager_notPointInTime() throws Exception {
    MetricData metricCount1 = new MetricData("metric1", false);
    metricCount1.value = 123;
    metricCount1.prevPublishValue = 0;
    metricCount1.publishCount = 0;
    
    MetricData metricCount2 = new MetricData("metric1", false);
    metricCount2.value = 123;
    metricCount2.prevPublishValue = 100;
    metricCount2.publishCount = 0;
    
    MetricData metricCount3 = new MetricData("metric1", false); // not included due to decrease of count
    metricCount3.value = 99;
    metricCount3.prevPublishValue = 100;
    metricCount3.publishCount = 1;
    
    manager.useMetrics(Arrays.asList(metricCount1, metricCount2, metricCount3));
    
    verify(mockClient);
    
    TimelineMetrics metrics = capture.getValue();
    List<TimelineMetric> metricList = metrics.getMetrics();
    assertEquals(metricList.size(), 1);
    
    TimelineMetric metric = metricList.get(0);
    assertEquals(metric.getAppId(), "logfeeder");
    assertEquals(metric.getMetricName(), "metric1");
    assertEquals(metric.getType(), "Long");
    
    TreeMap<Long, Double> values = metric.getMetricValues();
    assertEquals(values.size(), 1);
    assertEquals(values.firstEntry().getValue(), Double.valueOf(146.0));
  }
}
