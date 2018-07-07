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
package org.apache.ambari.metrics.core.timeline;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricHostMetadata;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.availability.MetricCollectorHAHelper;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.ssl.SslContextFactory;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_SECOND_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_APP_ID;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_COLLECTOR_IGNITE_BACKUPS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_COLLECTOR_IGNITE_NODES;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATION_SQL_FILTERS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_SERVICE_HTTP_POLICY;
import static org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils.getRoundedCheckPointTimeMillis;
import static org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils.getTimeSlices;
import static org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils.sliceFromTimelineMetric;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaMetricPatterns;

public class TimelineMetricsIgniteCache implements TimelineMetricDistributedCache {
  private static final Log LOG =
      LogFactory.getLog(TimelineMetricsIgniteCache.class);
  private IgniteCache<TimelineClusterMetric, MetricClusterAggregate> igniteCache;
  private long cacheSliceIntervalMillis;
  private boolean interpolationEnabled;
  private List<String> skipAggrPatternStrings = new ArrayList<>();
  private List<String> appIdsToAggregate;
  private TimelineMetricMetadataManager metricMetadataManager;


  public TimelineMetricsIgniteCache(TimelineMetricMetadataManager metricMetadataManager) throws MalformedURLException, URISyntaxException {
    TimelineMetricConfiguration timelineMetricConfiguration = TimelineMetricConfiguration.getInstance();
    Configuration metricConf = timelineMetricConfiguration.getMetricsConf();
    Configuration sslConf = timelineMetricConfiguration.getMetricsSslConf();

    IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
    this.metricMetadataManager = metricMetadataManager;

    //TODO add config to disable logging

    //enable ssl for ignite requests
    if (metricConf.get(TIMELINE_SERVICE_HTTP_POLICY) != null && metricConf.get(TIMELINE_SERVICE_HTTP_POLICY).equalsIgnoreCase("HTTPS_ONLY")) {
      SslContextFactory sslContextFactory = new SslContextFactory();
      String keyStorePath = sslConf.get("ssl.server.keystore.location");
      String keyStorePassword = sslConf.get("ssl.server.keystore.password");
      String trustStorePath = sslConf.get("ssl.server.truststore.location");
      String trustStorePassword = sslConf.get("ssl.server.truststore.password");

      sslContextFactory.setKeyStoreFilePath(keyStorePath);
      sslContextFactory.setKeyStorePassword(keyStorePassword.toCharArray());
      sslContextFactory.setTrustStoreFilePath(trustStorePath);
      sslContextFactory.setTrustStorePassword(trustStorePassword.toCharArray());
      igniteConfiguration.setSslContextFactory(sslContextFactory);
    }

    //aggregation parameters
    appIdsToAggregate = timelineMetricConfiguration.getAppIdsForHostAggregation();
    interpolationEnabled = Boolean.parseBoolean(metricConf.get(TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED, "true"));
    cacheSliceIntervalMillis = SECONDS.toMillis(metricConf.getInt(CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL, 30));
    Long aggregationInterval = metricConf.getLong(CLUSTER_AGGREGATOR_SECOND_SLEEP_INTERVAL, 120L);

    // Skip aggregation for metrics for which aggregating across hosts does not make sense.
    String filteredMetricPatterns = metricConf.get(TIMELINE_METRIC_AGGREGATION_SQL_FILTERS);
    if (StringUtils.isNotEmpty(filteredMetricPatterns)) {
      LOG.info("Skipping in memory cluster aggregation for metric patterns : " + filteredMetricPatterns);
      skipAggrPatternStrings.addAll(getJavaMetricPatterns(filteredMetricPatterns));
    }

    // Skip aggregation for those metrics that are meant to be of high volume and get differential treatment.
    String transientMetricPatterns = timelineMetricConfiguration.getTransientMetricPatterns();
    if (StringUtils.isNotEmpty(transientMetricPatterns)) {
      LOG.info("Skipping in memory cluster aggregation for transient metric patterns : " + transientMetricPatterns);
      skipAggrPatternStrings.addAll(getJavaMetricPatterns(transientMetricPatterns));
    }

    if (metricConf.get(TIMELINE_METRICS_COLLECTOR_IGNITE_NODES) != null) {
      TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
      TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
      ipFinder.setAddresses(Arrays.asList(metricConf.get(TIMELINE_METRICS_COLLECTOR_IGNITE_NODES).split(",")));
      LOG.info("Setting ignite nodes to : " + ipFinder.getRegisteredAddresses());
      discoverySpi.setIpFinder(ipFinder);
      igniteConfiguration.setDiscoverySpi(discoverySpi);
    } else {
      //get live nodes from ZK
      String zkClientPort = timelineMetricConfiguration.getClusterZKClientPort();
      String zkQuorum = timelineMetricConfiguration.getClusterZKQuorum();
      String zkConnectionURL = timelineMetricConfiguration.getZkConnectionUrl(zkClientPort, zkQuorum);
      MetricCollectorHAHelper metricCollectorHAHelper = new MetricCollectorHAHelper(zkConnectionURL, 5, 200);
      Collection<String> liveCollectors = metricCollectorHAHelper.findLiveCollectorHostsFromZNode();
      if (liveCollectors != null && !liveCollectors.isEmpty()) {
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(liveCollectors);
        LOG.info("Setting ignite nodes to : " + ipFinder.getRegisteredAddresses());
        discoverySpi.setIpFinder(ipFinder);
        igniteConfiguration.setDiscoverySpi(discoverySpi);
      }
    }


    //ignite cache configuration
    CacheConfiguration<TimelineClusterMetric, MetricClusterAggregate> cacheConfiguration = new CacheConfiguration<>();
    cacheConfiguration.setName("metrics_cache");
    //set cache mode to partitioned with # of backups
    cacheConfiguration.setCacheMode(CacheMode.PARTITIONED);
    cacheConfiguration.setBackups(metricConf.getInt(TIMELINE_METRICS_COLLECTOR_IGNITE_BACKUPS, 1));
    //disable throttling due to cpu impact
    cacheConfiguration.setRebalanceThrottle(0);
    //enable locks
    cacheConfiguration.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    //expiry policy to remove lost keys, if any
    cacheConfiguration.setEagerTtl(true);
    cacheConfiguration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, aggregationInterval * 3)));

    Ignite igniteNode = Ignition.start(igniteConfiguration);
    igniteCache = igniteNode.getOrCreateCache(cacheConfiguration);
  }

  /**
   * Looks through the cache and evicts all elements within (startTime; endTime] half-interval
   * All elements satisfying the half-interval will be removed from the cache.
   * @param startTime
   * @param endTime
   * @return
   */
  @Override
  public Map<TimelineClusterMetric, MetricClusterAggregate> evictMetricAggregates(Long startTime, Long endTime) {
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregatedMetricsMap = new HashMap<>();

    //construct filter
    IgniteBiPredicate<TimelineClusterMetric, MetricClusterAggregate> filter =
        (IgniteBiPredicate<TimelineClusterMetric, MetricClusterAggregate>) (key, value) -> key.getTimestamp() > startTime && key.getTimestamp() <= endTime;

    //get values from cache
    try (QueryCursor<Cache.Entry<TimelineClusterMetric, MetricClusterAggregate>> cursor = igniteCache.query(new ScanQuery(filter))) {
      for (Cache.Entry<TimelineClusterMetric, MetricClusterAggregate> e : cursor) {
        aggregatedMetricsMap.put(e.getKey(), e.getValue());
      }
    }

    //remove values from cache
    igniteCache.removeAllAsync(aggregatedMetricsMap.keySet());

    return aggregatedMetricsMap;
  }

  /**
   * Iterates through elements skipping white-listed patterns;
   * calculates average value for each slice of each metric (last slice values could be ignored in there is the possibility that values from this slice could be present in next post);
   * updates/adds the value in the cache;
   * calculates applications host metrics based on the metadata of hosted apps
   * updates metadata of hosted apps if needed
   * @param elements
   */
  @Override
  public void putMetrics(Collection<TimelineMetric> elements) {
    Map<String, TimelineMetricHostMetadata> hostMetadata = metricMetadataManager.getHostedAppsCache();
    for (TimelineMetric metric : elements) {
      if (shouldBeSkipped(metric.getMetricName())) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Skipping %s metric from being aggregated", metric.getMetricName()));
        }
        continue;
      }
      List<Long[]> timeSlices = getTimeSlices(getRoundedCheckPointTimeMillis(metric.getMetricValues().firstKey(), cacheSliceIntervalMillis), metric.getMetricValues().lastKey(), cacheSliceIntervalMillis);
      Map<TimelineClusterMetric, Double> slicedClusterMetrics = sliceFromTimelineMetric(metric, timeSlices, interpolationEnabled);

      if (slicedClusterMetrics != null) {
        for (Map.Entry<TimelineClusterMetric, Double> metricDoubleEntry : slicedClusterMetrics.entrySet()) {
          MetricClusterAggregate newMetricClusterAggregate  = new MetricClusterAggregate(
              metricDoubleEntry.getValue(), 1, null, metricDoubleEntry.getValue(), metricDoubleEntry.getValue());
          //put app metric into cache
          putMetricIntoCache(metricDoubleEntry.getKey(), newMetricClusterAggregate);
          if (hostMetadata != null) {
            //calculate app host metric
            if (metric.getAppId().equalsIgnoreCase(HOST_APP_ID)) {
              // Candidate metric, update app aggregates
              if (hostMetadata.containsKey(metric.getHostName())) {
                updateAppAggregatesFromHostMetric(metricDoubleEntry.getKey(), newMetricClusterAggregate, hostMetadata.get(metric.getHostName()));
              }
            } else {
              // Build the hostedapps map if not a host metric
              // Check app candidacy for host aggregation
              //TODO better to lock TimelineMetricHostMetadata instance to avoid dataloss, but generally the data could be lost only during initial collector start
              if (appIdsToAggregate.contains(metric.getAppId())) {
                TimelineMetricHostMetadata timelineMetricHostMetadata = hostMetadata.get(metric.getHostName());
                ConcurrentHashMap<String, String> appIdsMap;
                if (timelineMetricHostMetadata == null) {
                  appIdsMap = new ConcurrentHashMap<>();
                  hostMetadata.put(metric.getHostName(), new TimelineMetricHostMetadata(appIdsMap));
                } else {
                  appIdsMap = timelineMetricHostMetadata.getHostedApps();
                }
                if (!appIdsMap.containsKey(metric.getAppId())) {
                  appIdsMap.put(metric.getAppId(), metric.getAppId());
                  LOG.info("Adding appId to hosted apps: appId = " +
                      metric.getAppId() + ", hostname = " + metric.getHostName());
                }
              }
            }
          }
        }
      }
    }
  }

  private void updateAppAggregatesFromHostMetric(TimelineClusterMetric key, MetricClusterAggregate newMetricClusterAggregate, TimelineMetricHostMetadata timelineMetricHostMetadata) {
    for (String appId : timelineMetricHostMetadata.getHostedApps().keySet()) {
      if (appIdsToAggregate.contains(appId)) {
        TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(key.getMetricName(), appId, key.getInstanceId(), key.getTimestamp());

        TimelineMetricMetadataKey metadataKey = new TimelineMetricMetadataKey(timelineClusterMetric.getMetricName(), appId, timelineClusterMetric.getInstanceId());
        TimelineMetricMetadata metricMetadata = metricMetadataManager.getMetadataCacheValue(metadataKey);

        if (metricMetadata == null || metricMetadata.getUuid() == null) {
          TimelineMetricMetadataKey metricMetadataKey = new TimelineMetricMetadataKey(timelineClusterMetric.getMetricName(), HOST_APP_ID, timelineClusterMetric.getInstanceId());
          metricMetadata = metricMetadataManager.getMetadataCacheValue(metricMetadataKey);
          if (metricMetadata != null) {
            TimelineMetricMetadata timelineMetricMetadata = new TimelineMetricMetadata(timelineClusterMetric.getMetricName(),
              appId, timelineClusterMetric.getInstanceId(), metricMetadata.getUnits(), metricMetadata.getType(), metricMetadata.getSeriesStartTime(),
              metricMetadata.isSupportsAggregates(), TimelineMetricsFilter.acceptMetric(timelineClusterMetric.getMetricName(), appId));
            byte[] uuid = metricMetadataManager.getUuid(timelineClusterMetric.getMetricName(), appId, timelineClusterMetric.getInstanceId(), StringUtils.EMPTY, true);
            timelineMetricMetadata.setUuid(uuid);
            metricMetadataManager.putIfModifiedTimelineMetricMetadata(timelineMetricMetadata);
          }
        }

        putMetricIntoCache(timelineClusterMetric, newMetricClusterAggregate);
      }
    }
  }

  private void putMetricIntoCache(TimelineClusterMetric metricKey, MetricClusterAggregate metricValue) {
    Lock lock = igniteCache.lock(metricKey);
    lock.lock();
    try {
      MetricClusterAggregate metricClusterAggregateFromCache = igniteCache.get(metricKey);
      if (metricClusterAggregateFromCache == null) {
        igniteCache.put(metricKey, metricValue);
      } else {
        metricClusterAggregateFromCache.updateAggregates(metricValue);
        igniteCache.put(metricKey, metricClusterAggregateFromCache);
      }
    } catch (Exception e) {
      LOG.error("Exception : ", e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Map<String, Double> getPointInTimeCacheMetrics() {
    CacheMetrics clusterIgniteMetrics = igniteCache.metrics();
    Map<String, Double> metricsMap = new HashMap<>();
    metricsMap.put("Cluster_AverageGetTime", (double) clusterIgniteMetrics.getAverageGetTime());
    metricsMap.put("Cluster_AveragePutTime", (double) clusterIgniteMetrics.getAveragePutTime());
    metricsMap.put("Cluster_KeySize", (double) clusterIgniteMetrics.getKeySize());
    metricsMap.put("Cluster_OffHeapAllocatedSize", (double) clusterIgniteMetrics.getOffHeapAllocatedSize());
    return metricsMap;
  }

  private boolean shouldBeSkipped(String metricName) {
    for (String pattern : skipAggrPatternStrings) {
      if (metricName.matches(pattern)) {
        return true;
      }
    }
    return false;
  }
}
