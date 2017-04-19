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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetadataException;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DISABLE_METRIC_METADATA_MGMT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_INIT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_SCHEDULE_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_METADATA_FILTERS;

public class TimelineMetricMetadataManager {
  private static final Log LOG = LogFactory.getLog(TimelineMetricMetadataManager.class);
  private boolean isDisabled = false;
  // Cache all metadata on retrieval
  private final Map<TimelineMetricMetadataKey, TimelineMetricMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
  // Map to lookup apps on a host
  private final Map<String, Set<String>> HOSTED_APPS_MAP = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> INSTANCE_HOST_MAP = new ConcurrentHashMap<>();
  // Sync only when needed
  AtomicBoolean SYNC_HOSTED_APPS_METADATA = new AtomicBoolean(false);
  AtomicBoolean SYNC_HOSTED_INSTANCES_METADATA = new AtomicBoolean(false);

  // Single thread to sync back new writes to the store
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private PhoenixHBaseAccessor hBaseAccessor;
  private Configuration metricsConf;

  TimelineMetricMetadataSync metricMetadataSync;
  // Filter metrics names matching given patterns, from metadata
  final List<String> metricNameFilters = new ArrayList<>();

  public TimelineMetricMetadataManager(PhoenixHBaseAccessor hBaseAccessor,
                                       Configuration metricsConf) {
    this.hBaseAccessor = hBaseAccessor;
    this.metricsConf = metricsConf;

    String patternStrings = metricsConf.get(TIMELINE_METRIC_METADATA_FILTERS);
    if (!StringUtils.isEmpty(patternStrings)) {
      metricNameFilters.addAll(Arrays.asList(patternStrings.split(",")));
    }
  }

  /**
   * Initialize Metadata from the store
   */
  public void initializeMetadata() {
    if (metricsConf.getBoolean(DISABLE_METRIC_METADATA_MGMT, false)) {
      isDisabled = true;
    } else {
      metricMetadataSync = new TimelineMetricMetadataSync(this);
      // Schedule the executor to sync to store
      executorService.scheduleWithFixedDelay(metricMetadataSync,
        metricsConf.getInt(METRICS_METADATA_SYNC_INIT_DELAY, 120), // 2 minutes
        metricsConf.getInt(METRICS_METADATA_SYNC_SCHEDULE_DELAY, 300), // 5 minutes
        TimeUnit.SECONDS);
      // Read from store and initialize map
      try {
        Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata = getMetadataFromStore();

        LOG.info("Retrieved " + metadata.size() + ", metadata objects from store.");
        // Store in the cache
        METADATA_CACHE.putAll(metadata);

        Map<String, Set<String>> hostedAppData = getHostedAppsFromStore();

        LOG.info("Retrieved " + hostedAppData.size() + " host objects from store.");
        HOSTED_APPS_MAP.putAll(hostedAppData);

      } catch (SQLException e) {
        LOG.warn("Exception loading metric metadata", e);
      }
    }
  }

  public Map<TimelineMetricMetadataKey, TimelineMetricMetadata> getMetadataCache() {
    return METADATA_CACHE;
  }

  public TimelineMetricMetadata getMetadataCacheValue(TimelineMetricMetadataKey key) {
    return METADATA_CACHE.get(key);
  }

  public Map<String, Set<String>> getHostedAppsCache() {
    return HOSTED_APPS_MAP;
  }

  public Map<String, Set<String>> getHostedInstanceCache() {
    return INSTANCE_HOST_MAP;
  }

  public boolean syncHostedAppsMetadata() {
    return SYNC_HOSTED_APPS_METADATA.get();
  }

  public boolean syncHostedInstanceMetadata() {
    return SYNC_HOSTED_INSTANCES_METADATA.get();
  }

  public void markSuccessOnSyncHostedAppsMetadata() {
    SYNC_HOSTED_APPS_METADATA.set(false);
  }

  public void markSuccessOnSyncHostedInstanceMetadata() {
    SYNC_HOSTED_INSTANCES_METADATA.set(false);
  }
  /**
   * Test metric name for valid patterns and return true/false
   */
  boolean skipMetadataCache(String metricName) {
    for (String pattern : metricNameFilters) {
      if (metricName.contains(pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update value in metadata cache
   * @param metadata @TimelineMetricMetadata
   */
  public void putIfModifiedTimelineMetricMetadata(TimelineMetricMetadata metadata) {
    if (skipMetadataCache(metadata.getMetricName())) {
      return;
    }

    TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(
      metadata.getMetricName(), metadata.getAppId());

    TimelineMetricMetadata metadataFromCache = METADATA_CACHE.get(key);

    if (metadataFromCache != null) {
      try {
        if (metadataFromCache.needsToBeSynced(metadata)) {
          metadata.setIsPersisted(false); // Set the flag to ensure sync to store on next run
          METADATA_CACHE.put(key, metadata);
        }
      } catch (MetadataException e) {
        LOG.warn("Error inserting Metadata in cache.", e);
      }

    } else {
      METADATA_CACHE.put(key, metadata);
    }
  }

  /**
   * Update value in hosted apps cache
   * @param hostname Host name
   * @param appId Application Id
   */
  public void putIfModifiedHostedAppsMetadata(String hostname, String appId) {
    Set<String> apps = HOSTED_APPS_MAP.get(hostname);
    if (apps == null) {
      apps = new HashSet<>();
      HOSTED_APPS_MAP.put(hostname, apps);
    }

    if (!apps.contains(appId)) {
      apps.add(appId);
      SYNC_HOSTED_APPS_METADATA.set(true);
    }
  }

  public void putIfModifiedHostedInstanceMetadata(String instanceId, String hostname) {
    if (StringUtils.isEmpty(instanceId)) {
      return;
    }

    Set<String> hosts = INSTANCE_HOST_MAP.get(instanceId);
    if (hosts == null) {
      hosts = new HashSet<>();
      INSTANCE_HOST_MAP.put(instanceId, hosts);
    }

    if (!hosts.contains(hostname)) {
      hosts.add(hostname);
      SYNC_HOSTED_INSTANCES_METADATA.set(true);
    }
  }

  public void persistMetadata(Collection<TimelineMetricMetadata> metadata) throws SQLException {
    hBaseAccessor.saveMetricMetadata(metadata);
  }

  public void persistHostedAppsMetadata(Map<String, Set<String>> hostedApps) throws SQLException {
    hBaseAccessor.saveHostAppsMetadata(hostedApps);
  }

  public void persistHostedInstanceMetadata(Map<String, Set<String>> hostedInstancesMetadata) throws SQLException {
    hBaseAccessor.saveInstanceHostsMetadata(hostedInstancesMetadata);
  }

  public TimelineMetricMetadata getTimelineMetricMetadata(TimelineMetric timelineMetric, boolean isWhitelisted) {
    return new TimelineMetricMetadata(
      timelineMetric.getMetricName(),
      timelineMetric.getAppId(),
      timelineMetric.getUnits(),
      timelineMetric.getType(),
      timelineMetric.getStartTime(),
      supportAggregates(timelineMetric),
      isWhitelisted
    );
  }

  public boolean isDisabled() {
    return isDisabled;
  }

  boolean isDistributedModeEnabled() {
    return metricsConf.get("timeline.metrics.service.operation.mode", "").equals("distributed");
  }

  /**
   * Fetch metrics metadata from store
   * @throws SQLException
   */
  Map<TimelineMetricMetadataKey, TimelineMetricMetadata> getMetadataFromStore() throws SQLException {
    return hBaseAccessor.getTimelineMetricMetadata();
  }

  /**
   * Fetch hosted apps from store
   * @throws SQLException
   */
  Map<String, Set<String>> getHostedAppsFromStore() throws SQLException {
    return hBaseAccessor.getHostedAppsMetadata();
  }

  Map<String, Set<String>> getHostedInstancesFromStore() throws SQLException {
    return hBaseAccessor.getInstanceHostsMetdata();
  }

  private boolean supportAggregates(TimelineMetric metric) {
    return MapUtils.isEmpty(metric.getMetadata()) ||
      !(String.valueOf(true).equals(metric.getMetadata().get("skipAggregation")));
  }
}
