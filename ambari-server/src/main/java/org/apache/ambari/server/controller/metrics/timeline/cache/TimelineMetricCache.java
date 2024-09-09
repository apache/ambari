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
package org.apache.ambari.server.controller.metrics.timeline.cache;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.ehcache.Cache;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.spi.loaderwriter.CacheLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimelineMetricCache {
  private final Cache<TimelineAppMetricCacheKey, TimelineMetricsCacheValue> cache;
  private final DefaultStatisticsService statisticsService;
  private final TimelineMetricCacheEntryFactory cacheEntryFactory;
  public static final String TIMELINE_METRIC_CACHE_INSTANCE_NAME = "timelineMetricCache";
  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricCache.class);
  private static AtomicInteger printCacheStatsCounter = new AtomicInteger(0);

  /**
   * Creates a TimelineMetricCache.
   *
   * @param cache @Cache
   * @param cacheEntryFactory @CacheEntryFactory
   * @param statisticsService @DefaultStatisticsService
   */
  public TimelineMetricCache(Cache<TimelineAppMetricCacheKey, TimelineMetricsCacheValue> cache, TimelineMetricCacheEntryFactory cacheEntryFactory, DefaultStatisticsService statisticsService) {
    this.cache = cache;
    this.cacheEntryFactory = cacheEntryFactory;
    this.statisticsService = statisticsService;
  }

  /**
   * Get metrics for an app grouped by the requested @TemporalInfo which is a
   * part of the @TimelineAppMetricCacheKey
   * @param key @TimelineAppMetricCacheKey
   * @return @org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics
   */
  public TimelineMetrics getAppTimelineMetricsFromCache(TimelineAppMetricCacheKey key) throws IllegalArgumentException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Fetching metrics with key: {}", key);
    }

    // Make sure key is valid
    validateKey(key);

    TimelineMetricsCacheValue value = null;
    try {
      value = cache.get(key);
    } catch (CacheLoadingException cle) {
      Throwable t = cle.getCause();
      if(t instanceof SocketTimeoutException) {
        throw new SocketTimeoutException(t.getMessage());
      }
      if(t instanceof IOException) {
        throw new IOException(t.getMessage());
      }
      throw cle;
    }

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    if (value != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Returning value from cache: {}", value);
      }
      timelineMetrics = value.getTimelineMetrics();
    }

    if (LOG.isDebugEnabled()) {
      // Print stats every 100 calls - Note: Supported in debug mode only
      if (printCacheStatsCounter.getAndIncrement() == 0) {
        CacheStatistics cacheStatistics = statisticsService.getCacheStatistics(TIMELINE_METRIC_CACHE_INSTANCE_NAME);
        if(cacheStatistics == null) {
          LOG.warn("Cache statistics not available.");
          return timelineMetrics;
        }
        LOG.debug("Metrics cache stats => \n, Evictions = {}, Expired = {}, Hits = {}, Misses = {}, Hit ratio = {}, Puts = {}",
                cacheStatistics.getCacheEvictions(), cacheStatistics.getCacheExpirations(), cacheStatistics.getCacheHits(), cacheStatistics.getCacheMisses(), cacheStatistics.getCacheHitPercentage(), cacheStatistics.getCachePuts()
        );
      } else {
        printCacheStatsCounter.compareAndSet(100, 0);
      }
    }
    return timelineMetrics;
  }

  private void validateKey(TimelineAppMetricCacheKey key) throws IllegalArgumentException {
    StringBuilder msg = new StringBuilder("Invalid metric key requested.");
    boolean throwException = false;

    if (key.getTemporalInfo() == null) {
      msg.append(" No temporal info provided.");
      throwException = true;
    }

    if (key.getSpec() == null) {
      msg.append(" Missing call spec for metric request.");
    }

    if (throwException) {
      throw new IllegalArgumentException(msg.toString());
    }
  }
}
