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
package org.apache.ambari.server.metrics.system.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

public class JvmMetricsSource extends AbstractMetricsSource {
  static final MetricRegistry registry = new MetricRegistry();
  private static Logger LOG = LoggerFactory.getLogger(JvmMetricsSource.class);

  @Override
  public void init(MetricsConfiguration configuration, MetricsSink sink) {
    super.init(configuration, sink);
    registerAll("jvm.gc", new GarbageCollectorMetricSet(), registry);
    registerAll("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), registry);
    registerAll("jvm.memory", new MemoryUsageGaugeSet(), registry);
    registerAll("jvm.threads", new ThreadStatesGaugeSet(), registry);
    registry.register("jvm.file.open.descriptor.ratio", new FileDescriptorRatioGauge());
  }

  @Override
  public void run() {
    sink.publish(getMetrics());
    LOG.debug("********* Published system metrics to sink **********");
  }

  private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
    for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
      if (entry.getValue() instanceof MetricSet) {
        registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
      } else {
        registry.register(prefix + "." + entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public List<SingleMetric> getMetrics() {

    List<SingleMetric> metrics = new ArrayList<>();
    Map<String, Gauge> gaugeSet = registry.getGauges(new NonNumericMetricFilter());
    for (String metricName : gaugeSet.keySet()) {
      Number value = (Number) gaugeSet.get(metricName).getValue();
      metrics.add(new SingleMetric(metricName, value.doubleValue(), System.currentTimeMillis()));
    }

    return metrics;
  }

  public class NonNumericMetricFilter implements MetricFilter {

    @Override
    public boolean matches(String name, Metric metric) {
      if (name.equalsIgnoreCase("jvm.threads.deadlocks")) {
        return false;
      }
      return true;
    }
  }
}