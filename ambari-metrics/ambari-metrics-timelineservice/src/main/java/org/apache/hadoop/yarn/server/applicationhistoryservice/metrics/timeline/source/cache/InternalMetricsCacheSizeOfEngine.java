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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source.cache;

import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsEhCacheSizeOfEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;

public class InternalMetricsCacheSizeOfEngine {
// extends TimelineMetricsEhCacheSizeOfEngine {
//  private final static Logger LOG = LoggerFactory.getLogger(InternalMetricsCacheSizeOfEngine.class);
//
//  private InternalMetricsCacheSizeOfEngine(SizeOfEngine underlying) {
//    super(underlying);
//  }
//
//  public InternalMetricsCacheSizeOfEngine() {
//    // Invoke default constructor in base class
//  }
//
//  @Override
//  public Size sizeOf(Object key, Object value, Object container) {
//    try {
//      LOG.debug("BEGIN - Sizeof, key: {}, value: {}", key, value);
//      long size = 0;
//      if (key instanceof InternalMetricCacheKey) {
//        InternalMetricCacheKey metricCacheKey = (InternalMetricCacheKey) key;
//        size += reflectionSizeOf.sizeOf(metricCacheKey.getMetricName());
//        size += reflectionSizeOf.sizeOf(metricCacheKey.getAppId());
//        size += reflectionSizeOf.sizeOf(metricCacheKey.getInstanceId()); // null safe
//        size += reflectionSizeOf.sizeOf(metricCacheKey.getHostname());
//      }
//      if (value instanceof InternalMetricCacheValue) {
//        size += getValueMapSize(((InternalMetricCacheValue) value).getMetricValues());
//      }
//      // Mark size as not being exact
//      return new Size(size, false);
//    } finally {
//      LOG.debug("END - Sizeof, key: {}", key);
//    }
//  }
//
//  @Override
//  public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
//    LOG.debug("Copying tracing sizeof engine, maxdepth: {}, abort: {}",
//      maxDepth, abortWhenMaxDepthExceeded);
//
//    return new InternalMetricsCacheSizeOfEngine(underlying.copyWith(maxDepth, abortWhenMaxDepthExceeded));
//  }
}
