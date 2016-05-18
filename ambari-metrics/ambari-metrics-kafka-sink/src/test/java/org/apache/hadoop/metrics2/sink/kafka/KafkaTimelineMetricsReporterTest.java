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

package org.apache.hadoop.metrics2.sink.kafka;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import junit.framework.Assert;
import kafka.utils.VerifiableProperties;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.mock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Metrics.class, URL.class, OutputStream.class,
  KafkaTimelineMetricsReporter.TimelineScheduledReporter.class })
@PowerMockIgnore({"javax.management.*", "org.apache.log4j.*", "org.slf4j.*"})
public class KafkaTimelineMetricsReporterTest {

  private final List<Metric> list = new ArrayList<Metric>();
  private final MetricsRegistry registry = new MetricsRegistry();
  @SuppressWarnings("rawtypes")
  private final Gauge gauge = mock(Gauge.class);
  private final KafkaTimelineMetricsReporter kafkaTimelineMetricsReporter = new KafkaTimelineMetricsReporter();
  private VerifiableProperties props;

  @Before
  public void setUp() throws Exception {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Gauge g = registry.newGauge(System.class, "gauge", gauge);
    Counter counter = registry.newCounter(System.class, "counter");
    Histogram histogram = registry.newHistogram(System.class, "histogram");
    Meter meter = registry.newMeter(System.class, "meter", "empty", TimeUnit.MILLISECONDS);
    Timer timer = registry.newTimer(System.class, "timer");
    list.add(g);
    list.add(counter);
    list.add(histogram);
    list.add(meter);
    list.add(timer);
    Properties properties = new Properties();
    properties.setProperty("kafka.timeline.metrics.sendInterval", "5900");
    properties.setProperty("kafka.timeline.metrics.maxRowCacheSize", "10000");
    properties.setProperty("kafka.timeline.metrics.host", "localhost");
    properties.setProperty("kafka.timeline.metrics.port", "6188");
    properties.setProperty("kafka.timeline.metrics.reporter.enabled", "true");
    properties.setProperty("external.kafka.metrics.exclude.prefix", "a.b.c");
    properties.setProperty("external.kafka.metrics.include.prefix", "a.b.c.d");
    props = new VerifiableProperties(properties);
  }

  @Test
  public void testReporterStartStop() {
    mockStatic(Metrics.class);
    EasyMock.expect(Metrics.defaultRegistry()).andReturn(registry).times(2);
    TimelineMetricsCache timelineMetricsCache = getTimelineMetricsCache(kafkaTimelineMetricsReporter);
    kafkaTimelineMetricsReporter.setMetricsCache(timelineMetricsCache);
    replay(Metrics.class, timelineMetricsCache);
    kafkaTimelineMetricsReporter.init(props);
    kafkaTimelineMetricsReporter.stopReporter();
    verifyAll();
  }

  @Test
  public void testMetricsExclusionPolicy() throws Exception {
    mockStatic(Metrics.class);
    EasyMock.expect(Metrics.defaultRegistry()).andReturn(registry).times(2);
    TimelineMetricsCache timelineMetricsCache = getTimelineMetricsCache(kafkaTimelineMetricsReporter);
    kafkaTimelineMetricsReporter.setMetricsCache(timelineMetricsCache);

    replay(Metrics.class, timelineMetricsCache);
    kafkaTimelineMetricsReporter.init(props);

    Assert.assertTrue(kafkaTimelineMetricsReporter.isExcludedMetric("a.b.c"));
    Assert.assertFalse(kafkaTimelineMetricsReporter.isExcludedMetric("a.b"));
    Assert.assertFalse(kafkaTimelineMetricsReporter.isExcludedMetric("a.b.c.d"));
    Assert.assertFalse(kafkaTimelineMetricsReporter.isExcludedMetric("a.b.c.d.e"));

    kafkaTimelineMetricsReporter.stopReporter();
    verifyAll();
  }

  private TimelineMetricsCache getTimelineMetricsCache(KafkaTimelineMetricsReporter kafkaTimelineMetricsReporter) {
    TimelineMetricsCache timelineMetricsCache = EasyMock.createNiceMock(TimelineMetricsCache.class);
    kafkaTimelineMetricsReporter.setMetricsCache(timelineMetricsCache);
    EasyMock.expect(timelineMetricsCache.getTimelineMetric("key1")).andReturn(new TimelineMetric()).once();
    timelineMetricsCache.putTimelineMetric(EasyMock.anyObject(TimelineMetric.class));
    EasyMock.expectLastCall().once();
    return timelineMetricsCache;
  }

}
