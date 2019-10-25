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

package org.apache.hadoop.metrics2.sink.kafka;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.util.RatioGauge;

public class JvmMetricSet {

  private static final String GARBAGE_COLLECTOR = "gc";
  private static final String MEMORY = "memory";
  private static final String THREADS = "threads";
  private static final String RUNTIME = "runtime";


  private static final JvmMetricSet INSTANCE = new JvmMetricSet();


  public static JvmMetricSet getInstance() {
    return INSTANCE;
  }

  private final MemoryMXBean mxBean;
  private final ThreadMXBean threads;
  private final RuntimeMXBean runtimeBean;

  private class JvmMetric {
    private final MetricName metricName;
    private final Gauge<?> metric;

    JvmMetric(MetricName metricName, Gauge<?> metric) {
      this.metricName = metricName;
      this.metric = metric;
    }

    MetricName getMetricName() {
      return metricName;
    }

    Gauge<?> getMetric() {
      return metric;
    }
  }


  private JvmMetricSet() {
    this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getThreadMXBean(),
      ManagementFactory.getRuntimeMXBean());

  }

  private JvmMetricSet(MemoryMXBean mxBean, ThreadMXBean threads, RuntimeMXBean runtimeBean) {
    this.mxBean = mxBean;
    this.threads = threads;
    this.runtimeBean = runtimeBean;
  }

  public Map<MetricName, Gauge<?>> getJvmMetrics() {
    return Stream.concat(
      getMemoryUsageMetrics().stream(),
      Stream.concat(
        getThreadMetrics().stream(),
        Stream.of(getRuntimeMetrics())
      ))
      .collect(Collectors.toMap(JvmMetric::getMetricName, JvmMetric::getMetric));
  }

  private List<JvmMetric> getMemoryUsageMetrics() {

    return Stream.of(
      new AbstractMap.SimpleEntry<>("heap_usage", mxBean.getHeapMemoryUsage()),
      new AbstractMap.SimpleEntry<>("non_heap_usage", mxBean.getNonHeapMemoryUsage()))
      .map(entry ->
        new JvmMetric(
          MetricNameBuilder.builder().type(MEMORY).name(entry.getKey()).build(),
          new RatioGauge() {

            @Override
            protected double getNumerator() {
              return entry.getValue().getUsed();
            }

            @Override
            protected double getDenominator() {
              return entry.getValue().getMax();
            }
          }
        ))
      .collect(Collectors.toList());

  }

  private List<JvmMetric> getThreadMetrics() {

    return
      Stream.concat(
        Stream.of(
          new JvmMetric(
            MetricNameBuilder.builder().type(THREADS).name("thread_count").build(),
            new Gauge<Integer>() {
              @Override
              public Integer value() {
                return threads.getThreadCount();
              }
            }
          ),
          new JvmMetric(
            MetricNameBuilder.builder().type(THREADS).name("daemon_thread_count").build(),
            new Gauge<Integer>() {
              @Override
              public Integer value() {
                return threads.getDaemonThreadCount();
              }
            }
          )),
        Stream
          .of(Thread.State.RUNNABLE, Thread.State.BLOCKED, Thread.State.TIMED_WAITING, Thread.State.TERMINATED)
          .map(state -> new JvmMetric(
            MetricNameBuilder.builder().type(THREADS).name(getThreadMetricNameByState(state)).build(),
            new Gauge<Long>() {
              @Override
              public Long value() {
                return getThreadCountByState(state);
              }
            }
          )))
        .collect(Collectors.toList());
  }

  private String getThreadMetricNameByState(Thread.State state) {
    String name = "thread-states.";
    switch (state) {
      case BLOCKED:
        name += "blocked";
        break;
      case RUNNABLE:
        name += "runnable";
        break;
      case TIMED_WAITING:
        name += "timed_waiting";
        break;
      case TERMINATED:
        name += "terminated";
        break;
      case NEW:
        name += "new";
        break;
      case WAITING:
        name += "waiting";
        break;
    }
    return name;
  }

  private long getThreadCountByState(Thread.State state) {
    return Arrays.stream(threads.getThreadInfo(threads.getAllThreadIds(), 0))
      .filter(threadInfo -> threadInfo.getThreadState().equals(state))
      .count();
  }

  private JvmMetric getRuntimeMetrics() {
    return new JvmMetric(
      MetricNameBuilder.builder().type(RUNTIME).name("uptime").build(),
      new Gauge<Long>() {
        @Override
        public Long value() {
          return runtimeBean.getUptime();
        }
      }
    );
  }

}


