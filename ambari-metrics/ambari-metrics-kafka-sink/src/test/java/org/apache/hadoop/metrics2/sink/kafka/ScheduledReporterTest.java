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

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScheduledReporterTest {
  private final Gauge gauge = mock(Gauge.class);
  private final List<Metric> list = new ArrayList<Metric>();
  private final MetricsRegistry registry = new MetricsRegistry();
  private final ScheduledReporter reporter = spy(new ScheduledReporter(registry, "example", TimeUnit.SECONDS,
      TimeUnit.MILLISECONDS) {
    @Override
    public void report(Set<Entry<MetricName, Metric>> metrics) {
      // nothing doing!
    }
  });

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
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
    reporter.start(200, TimeUnit.MILLISECONDS);
  }

  @After
  public void tearDown() throws Exception {
    reporter.stop();
  }

  @Test
  public void pollsPeriodically() throws Exception {
    Thread.sleep(500);
    verify(reporter, times(2)).report(set(list));
  }

  private Set<Entry<MetricName, Metric>> set(List<Metric> metrics) {
    final Map<MetricName, Metric> map = new HashMap<MetricName, Metric>();
    for (Metric metric : metrics) {
      String name = null;
      if (metric instanceof Gauge) {
        name = "gauge";
      } else if (metric instanceof Counter) {
        name = "counter";
      } else if (metric instanceof Histogram) {
        name = "histogram";
      } else if (metric instanceof Meter) {
        name = "meter";
      } else if (metric instanceof Timer) {
        name = "timer";
      }
      map.put(new MetricName(System.class, name), metric);
    }
    return map.entrySet();
  }
}
