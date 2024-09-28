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

import java.time.Duration;

import org.apache.ambari.server.configuration.Configuration;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Cache implementation that provides ability to perform incremental reads
 * from Metrics backend and reduce the amount of calls between Ambari and the
 * Metrics backend.
 */
@Singleton
public class TimelineMetricCacheProvider {
  private TimelineMetricCache timelineMetricsCache;
  private volatile boolean isCacheInitialized = false;
  public static final String TIMELINE_METRIC_CACHE_INSTANCE_NAME = "timelineMetricCache";

  Configuration configuration;
  TimelineMetricCacheEntryFactory cacheEntryFactory;

  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricCacheProvider.class);

  @Inject
  public TimelineMetricCacheProvider(Configuration configuration,
                                     TimelineMetricCacheEntryFactory cacheEntryFactory) {
    this.configuration = configuration;
    this.cacheEntryFactory = cacheEntryFactory;
  }

  private synchronized void initializeCache() {
    if (isCacheInitialized) {
      return;
    }
    DefaultStatisticsService statisticsService = new DefaultStatisticsService();

    CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder()
            .using(statisticsService)
            .build(true);

    // Create a Cache specifying its configuration.
    CacheConfigurationBuilder<TimelineAppMetricCacheKey, TimelineMetricsCacheValue> cacheConfigurationBuilder = createCacheConfiguration();
    Cache<TimelineAppMetricCacheKey, TimelineMetricsCacheValue> cache = manager.createCache(TIMELINE_METRIC_CACHE_INSTANCE_NAME, cacheConfigurationBuilder);

    // Decorate with timelineMetricsCache.
    timelineMetricsCache = new TimelineMetricCache(cache, cacheEntryFactory, statisticsService);

    LOG.info("Registering metrics cache with provider: name = " +
      TIMELINE_METRIC_CACHE_INSTANCE_NAME + ", manager = " + manager);

    isCacheInitialized = true;
  }

  // Having this as a separate public method for testing/mocking purposes
  public CacheConfigurationBuilder createCacheConfiguration() {
    LOG.info("Creating Metrics Cache with timeouts => ttl = " +
      configuration.getMetricCacheTTLSeconds() + ", idle = " +
      configuration.getMetricCacheIdleSeconds() + ", cache size = " + configuration.getMetricCacheEntryUnitSize());

    TimelineMetricCacheCustomExpiry timelineMetricCacheCustomExpiry = new TimelineMetricCacheCustomExpiry(
            Duration.ofSeconds(configuration.getMetricCacheTTLSeconds()),
            Duration.ofSeconds(configuration.getMetricCacheIdleSeconds())
    );

    CacheConfigurationBuilder<TimelineAppMetricCacheKey, TimelineMetricsCacheValue> cacheConfigurationBuilder = CacheConfigurationBuilder
            .newCacheConfigurationBuilder(
                    TimelineAppMetricCacheKey.class,
                    TimelineMetricsCacheValue.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(configuration.getMetricCacheEntryUnitSize(), EntryUnit.ENTRIES)
            )
            .withKeySerializer(TimelineAppMetricCacheKeySerializer.class)
            .withValueSerializer(TimelineMetricsCacheValueSerializer.class)
            .withLoaderWriter(cacheEntryFactory)
            .withExpiry(timelineMetricCacheCustomExpiry);

    return cacheConfigurationBuilder;
  }

  /**
   * Return an instance of a Ehcache
   * @return @TimelineMetricCache or null if caching is disabled through config.
   */
  public TimelineMetricCache getTimelineMetricsCache() {
    if (configuration.isMetricsCacheDisabled()) {
      return null;
    }

    if (!isCacheInitialized) {
      initializeCache();
    }
    return timelineMetricsCache;
  }
}
