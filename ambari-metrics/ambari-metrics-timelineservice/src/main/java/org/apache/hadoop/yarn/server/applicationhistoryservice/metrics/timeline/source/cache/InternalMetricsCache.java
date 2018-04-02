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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class InternalMetricsCache {
  private static final Log LOG = LogFactory.getLog(InternalMetricsCache.class);
  private final String instanceName;
  private final String maxHeapPercent;
  private volatile boolean isCacheInitialized = false;
  private Cache cache;
  static final String TIMELINE_METRIC_CACHE_MANAGER_NAME = "internalMetricsCacheManager";
  private final Lock lock = new ReentrantLock();
  private static final int LOCK_TIMEOUT_SECONDS = 2;

  public InternalMetricsCache(String instanceName, String maxHeapPercent) {
    this.instanceName = instanceName;
    this.maxHeapPercent = maxHeapPercent;
    initialize();
  }

  private void initialize() {
    // Check in case of contention to avoid ObjectExistsException
    if (isCacheInitialized) {
      throw new RuntimeException("Cannot initialize internal cache twice");
    }

    System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    System.setProperty("net.sf.ehcache.sizeofengine." + TIMELINE_METRIC_CACHE_MANAGER_NAME,
      "org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source.cache.InternalMetricsCacheSizeOfEngine");

    net.sf.ehcache.config.Configuration managerConfig =
      new net.sf.ehcache.config.Configuration();
    managerConfig.setName(TIMELINE_METRIC_CACHE_MANAGER_NAME);

    // Set max heap available to the cache manager
    managerConfig.setMaxBytesLocalHeap(maxHeapPercent);

    //Create a singleton CacheManager using defaults
    CacheManager manager = CacheManager.create(managerConfig);

    LOG.info("Creating Metrics Cache with maxHeapPercent => " + maxHeapPercent);

    // Create a Cache specifying its configuration.
    CacheConfiguration cacheConfiguration = new CacheConfiguration()
      .name(instanceName)
      .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
      .sizeOfPolicy(new SizeOfPolicyConfiguration() // Set sizeOf policy to continue on max depth reached - avoid OOM
        .maxDepth(10000)
        .maxDepthExceededBehavior(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.CONTINUE))
      .eternal(true) // infinite time until eviction
      .persistence(new PersistenceConfiguration()
        .strategy(PersistenceConfiguration.Strategy.NONE.name()));

    cache = new Cache(cacheConfiguration);
    cache.getCacheEventNotificationService().registerListener(new InternalCacheEvictionListener());

    LOG.info("Registering internal metrics cache with provider: name = " +
      cache.getName() + ", guid: " + cache.getGuid());

    manager.addCache(cache);

    isCacheInitialized = true;
  }

  public InternalMetricCacheValue getInternalMetricCacheValue(InternalMetricCacheKey key) {
    Element ele = cache.get(key);
    if (ele != null) {
      return (InternalMetricCacheValue) ele.getObjectValue();
    }
    return null;
  }

  public Collection<TimelineMetrics> evictAll() {
    TimelineMetrics metrics = new TimelineMetrics();
    try {
      if (lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        try{
          List keys = cache.getKeys();
          for (Object obj : keys) {
            TimelineMetric metric = new TimelineMetric();
            InternalMetricCacheKey key = (InternalMetricCacheKey) obj;
            metric.setMetricName(key.getMetricName());
            metric.setAppId(key.getAppId());
            metric.setInstanceId(key.getInstanceId());
            metric.setHostName(key.getHostname());
            metric.setStartTime(key.getStartTime());
            Element ele = cache.get(key);
            metric.setMetricValues(((InternalMetricCacheValue) ele.getObjectValue()).getMetricValues());
            metrics.getMetrics().add(metric);
          }
          cache.removeAll();
        } finally {
          lock.unlock();
        }
      } else {
        LOG.warn("evictAll: Unable to acquire lock on the cache instance. " +
          "Giving up after " + LOCK_TIMEOUT_SECONDS + " seconds.");
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting to acquire lock");
    }

    return Collections.singletonList(metrics);
  }

  public void putAll(Collection<TimelineMetrics> metrics) {
    try {
      if (lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        try {
          if (metrics != null) {
            for (TimelineMetrics timelineMetrics : metrics) {
              for (TimelineMetric timelineMetric : timelineMetrics.getMetrics()) {
                InternalMetricCacheKey key = new InternalMetricCacheKey(
                  timelineMetric.getMetricName(),
                  timelineMetric.getAppId(),
                  timelineMetric.getInstanceId(),
                  timelineMetric.getHostName(),
                  timelineMetric.getStartTime()
                );

                Element ele = cache.get(key);
                if (ele != null) {
                  InternalMetricCacheValue value = (InternalMetricCacheValue) ele.getObjectValue();
                  value.addMetricValues(timelineMetric.getMetricValues());
                } else {
                  InternalMetricCacheValue value = new InternalMetricCacheValue();
                  value.setMetricValues(timelineMetric.getMetricValues());
                  cache.put(new Element(key, value));
                }
              }
            }
          }
        } finally {
          lock.unlock();
        }
      } else {
        LOG.warn("putAll: Unable to acquire lock on the cache instance. " +
          "Giving up after " + LOCK_TIMEOUT_SECONDS + " seconds.");
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting to acquire lock");
    }
  }

  class InternalCacheEvictionListener implements CacheEventListener {

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      // expected
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      // do nothing
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // do nothing
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {
      // do nothing
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
      // Bad - Remote endpoint cannot keep up resulting in flooding
      InternalMetricCacheKey key = (InternalMetricCacheKey) element.getObjectKey();
      LOG.warn("Evicting element from internal metrics cache, metric => " + key
        .getMetricName() + ", startTime = " + new Date(key.getStartTime()));
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {
      // expected
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return null;
    }

    @Override
    public void dispose() {
      // do nothing
    }
  }
}
