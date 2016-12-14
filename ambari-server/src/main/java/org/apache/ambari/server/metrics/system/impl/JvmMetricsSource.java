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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.apache.ambari.server.metrics.system.AmbariMetricSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class JvmMetricsSource extends AbstractMetricsSource {
  static final MetricRegistry registry = new MetricRegistry();
  private static Logger LOG = LoggerFactory.getLogger(JvmMetricsSource.class);

  @Override
  public void init(AmbariMetricSink sink) {
    super.init(sink);
    registerAll("gc", new GarbageCollectorMetricSet(), registry);
    registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), registry);
    registerAll("memory", new MemoryUsageGaugeSet(), registry);
    registerAll("threads", new ThreadStatesGaugeSet(), registry);
  }

  @Override
  public void run() {
    this.sink.publish(getMetrics());
    LOG.info("********* Published system metrics to sink **********");
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
  public Map<String, Number> getMetrics() {
    Map<String, Number> map = new HashMap<>();
    for (String metricName : registry.getGauges().keySet()) {
      if (metricName.equals("threads.deadlocks") ) continue;
      Number value = (Number)registry.getGauges().get(metricName).getValue();
      map.put(metricName, value);
    }
    return map;
  }
}
