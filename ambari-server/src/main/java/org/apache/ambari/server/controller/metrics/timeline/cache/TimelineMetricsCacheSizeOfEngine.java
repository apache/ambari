/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics.timeline.cache;

import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsEhCacheSizeOfEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache sizing engine that reduces reflective calls over the Object graph to
 * find total Heap usage.
 */
public class TimelineMetricsCacheSizeOfEngine extends TimelineMetricsEhCacheSizeOfEngine {

  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricsCacheSizeOfEngine.class);

  public TimelineMetricsCacheSizeOfEngine() {
    // Invoke default constructor in base class
  }

  @Override
  public long getSizeOfEntry(Object key, Object value) {
    try {
      LOG.debug("BEGIN - Sizeof, key: {}, value: {}", key, value);

      long size = 0;

      if (key instanceof TimelineAppMetricCacheKey) {
        size += getTimelineMetricCacheKeySize((TimelineAppMetricCacheKey) key);
      }

      if (value instanceof TimelineMetricsCacheValue) {
        size += getTimelineMetricCacheValueSize((TimelineMetricsCacheValue) value);
      }
      // Mark size as not being exact
      return size;
    } finally {
      LOG.debug("END - Sizeof, key: {}", key);
    }
  }

  private long getTimelineMetricCacheKeySize(TimelineAppMetricCacheKey key) {
    long size = reflectionSizeOf.sizeOf(key.getAppId());
    size += key.getMetricNames() != null && !key.getMetricNames().isEmpty() ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getMetricNames()).getCalculated() : 0;
    size += key.getSpec() != null ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getSpec()).getCalculated() : 0;
    size += key.getHostNames() != null ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getHostNames()).getCalculated() : 0;
    // 4 fixed longs of @TemporalInfo + reference
    size += 40;
    size += 8; // Object overhead

    return size;
  }

  private long getTimelineMetricCacheValueSize(TimelineMetricsCacheValue value) {
    long size = 16; // startTime + endTime

    size += 8; // Object reference

    size += getTimelineMetricsSize(value.getTimelineMetrics()); // TreeMap

    return size;
  }


}
