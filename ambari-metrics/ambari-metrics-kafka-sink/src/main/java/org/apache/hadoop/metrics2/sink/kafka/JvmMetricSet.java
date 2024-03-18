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
import javax.annotation.Nonnull;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.util.RatioGauge;

public class JvmMetricSet {

  private static final String MEMORY = "memory";
  private static final String THREADS = "threads";
  private static final String RUNTIME = "runtime";

  private static final JvmMetricSet INSTANCE = new JvmMetricSet();


  public static JvmMetricSet getInstance() {
    return INSTANCE;
  }

  private final MemoryMXBean memoryMXBean;
  private final ThreadMXBean threadMXBean;
  private final RuntimeMXBean runtimeMXBean;

  private static class JvmMetric {
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

  private JvmMetricSet(MemoryMXBean memoryMXBean, ThreadMXBean threadMXBean, RuntimeMXBean runtimeMXBean) {
    this.memoryMXBean = memoryMXBean;
    this.threadMXBean = threadMXBean;
    this.runtimeMXBean = runtimeMXBean;
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
      new AbstractMap.SimpleEntry<>("heap_usage", memoryMXBean.getHeapMemoryUsage()),
      new AbstractMap.SimpleEntry<>("non_heap_usage", memoryMXBean.getNonHeapMemoryUsage()))
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
                return threadMXBean.getThreadCount();
              }
            }
          ),
          new JvmMetric(
            MetricNameBuilder.builder().type(THREADS).name("daemon_thread_count").build(),
            new Gauge<Integer>() {
              @Override
              public Integer value() {
                return threadMXBean.getDaemonThreadCount();
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

  private String getThreadMetricNameByState(@Nonnull Thread.State state) {
    return String.format("thread-states.%s", state.name().toLowerCase());
  }

  private long getThreadCountByState(@Nonnull Thread.State state) {
    return Arrays.stream(threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 0))
      .filter(threadInfo -> threadInfo.getThreadState().equals(state))
      .count();
  }

  private JvmMetric getRuntimeMetrics() {
    return new JvmMetric(
      MetricNameBuilder.builder().type(RUNTIME).name("uptime").build(),
      new Gauge<Long>() {
        @Override
        public Long value() {
          return runtimeMXBean.getUptime();
        }
      }
    );
  }

}


