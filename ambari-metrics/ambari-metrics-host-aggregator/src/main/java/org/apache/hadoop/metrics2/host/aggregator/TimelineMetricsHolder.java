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
package org.apache.hadoop.metrics2.host.aggregator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Singleton class with 2 guava caches for raw and aggregated metrics storing
 */
public class TimelineMetricsHolder {
  private static final int DEFAULT_RAW_CACHE_EXPIRE_TIME = 60;
  private static final int DEFAULT_AGGREGATION_CACHE_EXPIRE_TIME = 300;
  private Cache<String, TimelineMetrics> aggregationMetricsCache;
  private Cache<String, TimelineMetrics> rawMetricsCache;
  private static TimelineMetricsHolder instance = null;
  //to ensure no metric values are expired
  private static int EXPIRE_DELAY = 30;
  ReadWriteLock aggregationCacheLock = new ReentrantReadWriteLock();
  ReadWriteLock rawCacheLock = new ReentrantReadWriteLock();
  private List<String> skipAggregationPatternStrings = new ArrayList<>();

  private TimelineMetricsHolder(int rawCacheExpireTime, int aggregationCacheExpireTime, List<String> skipAggregationPatternStrings) {
    this.rawMetricsCache = CacheBuilder.newBuilder().expireAfterWrite(rawCacheExpireTime + EXPIRE_DELAY, TimeUnit.SECONDS).build();
    this.aggregationMetricsCache = CacheBuilder.newBuilder().expireAfterWrite(aggregationCacheExpireTime + EXPIRE_DELAY, TimeUnit.SECONDS).build();
    this.skipAggregationPatternStrings = skipAggregationPatternStrings;
  }

  public static TimelineMetricsHolder getInstance(int rawCacheExpireTime, int aggregationCacheExpireTime, List<String> skipAggregationPatternStrings) {
    if (instance == null) {
      instance = new TimelineMetricsHolder(rawCacheExpireTime, aggregationCacheExpireTime, skipAggregationPatternStrings);
    }
    return instance;
  }

  /**
   * Uses default expiration time for caches initialization if they are not initialized yet.
   * @return
   */
  public static TimelineMetricsHolder getInstance() {
    return getInstance(DEFAULT_RAW_CACHE_EXPIRE_TIME, DEFAULT_AGGREGATION_CACHE_EXPIRE_TIME, Collections.emptyList());
  }

  public void putMetricsForAggregationPublishing(TimelineMetrics timelineMetrics) {

    //Remove metrics that need to be skipped during caching stage itself.
    for (Iterator<TimelineMetric> iterator = timelineMetrics.getMetrics().iterator(); iterator.hasNext();) {
      TimelineMetric timelineMetric = iterator.next();
      for (String pattern : skipAggregationPatternStrings) {
        if (timelineMetric.getMetricName().matches(pattern)) {
          iterator.remove();
        }
      }
    }
    aggregationCacheLock.writeLock().lock();
    aggregationMetricsCache.put(calculateCacheKey(timelineMetrics), timelineMetrics);
    aggregationCacheLock.writeLock().unlock();
  }

  private String calculateCacheKey(TimelineMetrics timelineMetrics) {
    List<TimelineMetric> metrics = timelineMetrics.getMetrics();
    if (metrics.size() > 0) {
      return metrics.get(0).getAppId() + System.currentTimeMillis();
    }
    return String.valueOf(System.currentTimeMillis());
  }

  public Map<String, TimelineMetrics> extractMetricsForAggregationPublishing() {
    return extractMetricsFromCacheWithLock(aggregationMetricsCache, aggregationCacheLock);
  }

  public void putMetricsForRawPublishing(TimelineMetrics metrics) {
    rawCacheLock.writeLock().lock();
    rawMetricsCache.put(calculateCacheKey(metrics), metrics);
    rawCacheLock.writeLock().unlock();
  }

  public Map<String, TimelineMetrics> extractMetricsForRawPublishing() {
    return extractMetricsFromCacheWithLock(rawMetricsCache, rawCacheLock);
  }

  /**
   * Returns values from cache and clears the cache
   * @param cache
   * @param lock
   * @return
   */
  private Map<String, TimelineMetrics> extractMetricsFromCacheWithLock(Cache<String, TimelineMetrics> cache, ReadWriteLock lock) {
    lock.writeLock().lock();
    Map<String, TimelineMetrics> metricsMap = new TreeMap<>(cache.asMap());
    cache.invalidateAll();
    lock.writeLock().unlock();
    return metricsMap;
  }

}
