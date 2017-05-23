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
package org.apache.hadoop.metrics2.sink.timeline.cache;

import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;

/**
 * Cache sizing engine that reduces reflective calls over the Object graph to
 * find total Heap usage. Used for ehcache based on available memory.
 */
public abstract class TimelineMetricsEhCacheSizeOfEngine implements SizeOfEngine {
  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricsEhCacheSizeOfEngine.class);

  private static int DEFAULT_MAX_DEPTH = 1000;
  private static boolean DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED = false;

  // Base Engine
  protected SizeOfEngine underlying = null;

  // Counter
  protected SizeOf reflectionSizeOf = new ReflectionSizeOf();

  // Optimizations
  private volatile long timelineMetricPrimitivesApproximation = 0;

  // Map entry sizing
  private long sizeOfMapEntry;
  private long sizeOfMapEntryOverhead;

  protected TimelineMetricsEhCacheSizeOfEngine(SizeOfEngine underlying) {
    this.underlying = underlying;
  }

  public TimelineMetricsEhCacheSizeOfEngine() {
    this(new DefaultSizeOfEngine(DEFAULT_MAX_DEPTH, DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED));

    this.sizeOfMapEntry = reflectionSizeOf.sizeOf(new Long(1)) +
      reflectionSizeOf.sizeOf(new Double(2.0));

    //SizeOfMapEntryOverhead = SizeOfMapWithOneEntry - (SizeOfEmptyMap + SizeOfOneEntry)
    TreeMap<Long, Double> map = new TreeMap<>();
    long emptyMapSize = reflectionSizeOf.sizeOf(map);
    map.put(new Long(1), new Double(2.0));
    long sizeOfMapOneEntry = reflectionSizeOf.deepSizeOf(DEFAULT_MAX_DEPTH, DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED, map).getCalculated();
    this.sizeOfMapEntryOverhead =  sizeOfMapOneEntry - (emptyMapSize + this.sizeOfMapEntry);

    LOG.info("Creating custom sizeof engine for TimelineMetrics.");
  }

  /**
   * Return size of the metrics TreeMap in an optimized way.
   *
   */
  protected long getTimelineMetricsSize(TimelineMetrics metrics) {
    long size = 8; // Object reference

    if (metrics != null) {
      for (TimelineMetric metric : metrics.getMetrics()) {

        if (timelineMetricPrimitivesApproximation == 0) {
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getMetricName());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getAppId());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getHostName());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getInstanceId());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getTimestamp());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getStartTime());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getType());
          timelineMetricPrimitivesApproximation += 8; // Object overhead

          LOG.debug("timelineMetricPrimitivesApproximation bytes = " + timelineMetricPrimitivesApproximation);
        }
        size += timelineMetricPrimitivesApproximation;
        size += getValueMapSize(metric.getMetricValues());
      }
      LOG.debug("Total Size of metric values in cache: " + size);
    }
    return size;
  }

  protected long getValueMapSize(Map<Long, Double> metricValues) {
    long size = 0;
    if (metricValues != null && !metricValues.isEmpty()) {
      // Numeric wrapper: 12 bytes + 8 bytes Data type + 4 bytes alignment = 48 (Long, Double)
      // Tree Map: 12 bytes for header + 20 bytes for 5 object fields : pointers + 1 byte for flag = 40
      LOG.debug("Size of metric value: " + (sizeOfMapEntry + sizeOfMapEntryOverhead) * metricValues.size());
      size += (sizeOfMapEntry + sizeOfMapEntryOverhead) * metricValues.size(); // Treemap size is O(1)
    }
    return size;
  }
}
