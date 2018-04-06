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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DISABLE_METRIC_METADATA_MGMT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_INIT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_SCHEDULE_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_UUID_GEN_STRATEGY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_METADATA_FILTERS;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetadataException;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.uuid.HashBasedUuidGenStrategy;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.uuid.MetricUuidGenStrategy;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.uuid.RandomUuidGenStrategy;

public class TimelineMetricMetadataManager {
  private static final Log LOG = LogFactory.getLog(TimelineMetricMetadataManager.class);
  private boolean isDisabled = false;
  // Cache all metadata on retrieval
  private final Map<TimelineMetricMetadataKey, TimelineMetricMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
  private final Map<String, TimelineMetricMetadataKey> uuidKeyMap = new ConcurrentHashMap<>();
  // Map to lookup apps on a host
  private final Map<String, TimelineMetricHostMetadata> HOSTED_APPS_MAP = new ConcurrentHashMap<>();
  private final Map<String, String> uuidHostMap = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> INSTANCE_HOST_MAP = new ConcurrentHashMap<>();
  // Sync only when needed
  AtomicBoolean SYNC_HOSTED_APPS_METADATA = new AtomicBoolean(false);
  AtomicBoolean SYNC_HOSTED_INSTANCES_METADATA = new AtomicBoolean(false);
  private MetricUuidGenStrategy uuidGenStrategy = new HashBasedUuidGenStrategy();
  public static final int TIMELINE_METRIC_UUID_LENGTH = 16;
  public static final int HOSTNAME_UUID_LENGTH = 4;

  // Single thread to sync back new writes to the store
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private PhoenixHBaseAccessor hBaseAccessor;
  private Configuration metricsConf;

  TimelineMetricMetadataSync metricMetadataSync;
  // Filter metrics names matching given patterns, from metadata
  final List<String> metricNameFilters = new ArrayList<>();

  // Test friendly construction since mock instrumentation is difficult to get
  // working with hadoop mini cluster
  public TimelineMetricMetadataManager(Configuration metricsConf, PhoenixHBaseAccessor hBaseAccessor) {
    this.metricsConf = metricsConf;
    this.hBaseAccessor = hBaseAccessor;
    String patternStrings = metricsConf.get(TIMELINE_METRIC_METADATA_FILTERS);
    if (!StringUtils.isEmpty(patternStrings)) {
      metricNameFilters.addAll(Arrays.asList(patternStrings.split(",")));
    }

    uuidGenStrategy = getUuidStrategy(metricsConf);
  }

  public TimelineMetricMetadataManager(PhoenixHBaseAccessor hBaseAccessor) throws MalformedURLException, URISyntaxException {
    this(TimelineMetricConfiguration.getInstance().getMetricsConf(), hBaseAccessor);
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

        Map<String, TimelineMetricHostMetadata> hostedAppData = getHostedAppsFromStore();

        LOG.info("Retrieved " + hostedAppData.size() + " host objects from store.");
        HOSTED_APPS_MAP.putAll(hostedAppData);

        loadUuidMapsOnInit();

        hBaseAccessor.setMetadataInstance(this);
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

  public Map<String, TimelineMetricHostMetadata> getHostedAppsCache() {
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
      metadata.getMetricName(), metadata.getAppId(), metadata.getInstanceId());

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
    TimelineMetricHostMetadata timelineMetricHostMetadata = HOSTED_APPS_MAP.get(hostname);
    ConcurrentHashMap<String, String> apps = (timelineMetricHostMetadata != null) ? timelineMetricHostMetadata.getHostedApps() : null;
    if (apps == null) {
      apps = new ConcurrentHashMap<>();
      if (timelineMetricHostMetadata == null) {
        HOSTED_APPS_MAP.put(hostname, new TimelineMetricHostMetadata(apps));
      } else {
        HOSTED_APPS_MAP.get(hostname).setHostedApps(apps);
      }
    }

    if (!apps.containsKey(appId)) {
      apps.put(appId, appId);
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

  public void persistHostedAppsMetadata(Map<String, TimelineMetricHostMetadata> hostedApps) throws SQLException {
    hBaseAccessor.saveHostAppsMetadata(hostedApps);
  }

  public void persistHostedInstanceMetadata(Map<String, Set<String>> hostedInstancesMetadata) throws SQLException {
    hBaseAccessor.saveInstanceHostsMetadata(hostedInstancesMetadata);
  }

  public TimelineMetricMetadata getTimelineMetricMetadata(TimelineMetric timelineMetric, boolean isWhitelisted) {
    return new TimelineMetricMetadata(
      timelineMetric.getMetricName(),
      timelineMetric.getAppId(),
      timelineMetric.getInstanceId(),
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
    return metricsConf.get("timeline.metrics.service.operation.mode").equals("distributed");
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
  Map<String, TimelineMetricHostMetadata> getHostedAppsFromStore() throws SQLException {
    return hBaseAccessor.getHostedAppsMetadata();
  }

  Map<String, Set<String>> getHostedInstancesFromStore() throws SQLException {
    return hBaseAccessor.getInstanceHostsMetdata();
  }

  private boolean supportAggregates(TimelineMetric metric) {
    return MapUtils.isEmpty(metric.getMetadata()) ||
      !(String.valueOf(true).equals(metric.getMetadata().get("skipAggregation")));
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // UUID Management
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  /**
   * Load the UUID mappings from the UUID table on startup.
   */
  private void loadUuidMapsOnInit() {

    for (TimelineMetricMetadataKey key : METADATA_CACHE.keySet()) {
      TimelineMetricMetadata timelineMetricMetadata = METADATA_CACHE.get(key);
      if (timelineMetricMetadata != null && timelineMetricMetadata.getUuid() != null) {
        uuidKeyMap.put(new String(timelineMetricMetadata.getUuid()), key);
      }
    }

    for (String host : HOSTED_APPS_MAP.keySet()) {
      TimelineMetricHostMetadata timelineMetricHostMetadata = HOSTED_APPS_MAP.get(host);
      if (timelineMetricHostMetadata != null && timelineMetricHostMetadata.getUuid() != null) {
        uuidHostMap.put(new String(timelineMetricHostMetadata.getUuid()), host);
      }
    }
  }

  /**
   * Returns the UUID gen strategy.
   * @param configuration
   * @return
   */
  private MetricUuidGenStrategy getUuidStrategy(Configuration configuration) {
    String strategy = configuration.get(TIMELINE_METRICS_UUID_GEN_STRATEGY, "");
    if ("random".equalsIgnoreCase(strategy)) {
      return new RandomUuidGenStrategy();
    } else {
      return new HashBasedUuidGenStrategy();
    }
  }

  /**
   * Given the hostname, generates a byte array of length 'HOSTNAME_UUID_LENGTH'
   * @param hostname
   * @return uuid byte array of length 'HOSTNAME_UUID_LENGTH'
   */
  private byte[] getUuidForHostname(String hostname) {

    TimelineMetricHostMetadata timelineMetricHostMetadata = HOSTED_APPS_MAP.get(hostname);
    if (timelineMetricHostMetadata != null) {
      byte[] uuid = timelineMetricHostMetadata.getUuid();
      if (uuid != null) {
        return uuid;
      }
    }

    byte[] uuid = uuidGenStrategy.computeUuid(hostname, HOSTNAME_UUID_LENGTH);

    String uuidStr = new String(uuid);
    if (uuidHostMap.containsKey(uuidStr)) {
      //TODO fix the collisions
      LOG.error("Duplicate key computed for " + hostname +", Collides with  " + uuidHostMap.get(uuidStr));
      return uuid;
    }

    if (timelineMetricHostMetadata == null) {
      timelineMetricHostMetadata = new TimelineMetricHostMetadata();
      HOSTED_APPS_MAP.put(hostname, timelineMetricHostMetadata);
    }
    timelineMetricHostMetadata.setUuid(uuid);
    uuidHostMap.put(uuidStr, hostname);

    return uuid;
  }

  /**
   * Given a timelineClusterMetric instance, generates a UUID for Metric-App-Instance combination.
   * @param timelineClusterMetric
   * @return uuid byte array of length 'TIMELINE_METRIC_UUID_LENGTH'
   */
  public byte[] getUuid(TimelineClusterMetric timelineClusterMetric) {
    TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(timelineClusterMetric.getMetricName(),
      timelineClusterMetric.getAppId(), timelineClusterMetric.getInstanceId());

    TimelineMetricMetadata timelineMetricMetadata = METADATA_CACHE.get(key);
    if (timelineMetricMetadata != null) {
      byte[] uuid = timelineMetricMetadata.getUuid();
      if (uuid != null) {
        return uuid;
      }
    }

    byte[] uuid = uuidGenStrategy.computeUuid(timelineClusterMetric, TIMELINE_METRIC_UUID_LENGTH);

    String uuidStr = new String(uuid);
    if (uuidKeyMap.containsKey(uuidStr) && !uuidKeyMap.get(uuidStr).equals(key)) {
      TimelineMetricMetadataKey collidingKey = (TimelineMetricMetadataKey)uuidKeyMap.get(uuidStr);
      //TODO fix the collisions
      /**
       * 2017-08-23 14:12:35,922 ERROR org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager:
       * Duplicate key [52, 50, 51, 53, 50, 53, 53, 53, 49, 54, 57, 50, 50, 54, 0, 0]([B@278a93f9) computed for
       * TimelineClusterMetric{metricName='sdisk_dm-11_write_count', appId='hbase', instanceId='', timestamp=1503497400000}, Collides with
       * TimelineMetricMetadataKey{metricName='sdisk_dm-20_write_count', appId='hbase', instanceId=''}
       */
      LOG.error("Duplicate key " + Arrays.toString(uuid) + "(" + uuid +  ") computed for " + timelineClusterMetric.toString() + ", Collides with  " + collidingKey.toString());
      return uuid;
    }

    if (timelineMetricMetadata == null) {
      timelineMetricMetadata = new TimelineMetricMetadata();
      timelineMetricMetadata.setMetricName(timelineClusterMetric.getMetricName());
      timelineMetricMetadata.setAppId(timelineClusterMetric.getAppId());
      timelineMetricMetadata.setInstanceId(timelineClusterMetric.getInstanceId());
      METADATA_CACHE.put(key, timelineMetricMetadata);
    }

    timelineMetricMetadata.setUuid(uuid);
    timelineMetricMetadata.setIsPersisted(false);
    uuidKeyMap.put(uuidStr, key);
    return uuid;
  }

  /**
   * Given a timelineMetric instance, generates a UUID for Metric-App-Instance combination.
   * @param timelineMetric
   * @return uuid byte array of length 'TIMELINE_METRIC_UUID_LENGTH' + 'HOSTNAME_UUID_LENGTH'
   */
  public byte[] getUuid(TimelineMetric timelineMetric) {

    byte[] metricUuid = getUuid(new TimelineClusterMetric(timelineMetric.getMetricName(), timelineMetric.getAppId(),
      timelineMetric.getInstanceId(), -1l));
    byte[] hostUuid = getUuidForHostname(timelineMetric.getHostName());

    return ArrayUtils.addAll(metricUuid, hostUuid);
  }

  public byte[] getUuid(String metricName, String appId, String instanceId, String hostname) {

    byte[] metricUuid = getUuid(new TimelineClusterMetric(metricName, appId, instanceId, -1l));
    if (StringUtils.isNotEmpty(hostname)) {
      byte[] hostUuid = getUuidForHostname(hostname);
      return ArrayUtils.addAll(metricUuid, hostUuid);
    }
    return metricUuid;
  }

  public String getMetricNameFromUuid(byte[]  uuid) {

    byte[] metricUuid = uuid;
    if (uuid.length == TIMELINE_METRIC_UUID_LENGTH + HOSTNAME_UUID_LENGTH) {
      metricUuid = ArrayUtils.subarray(uuid, 0, TIMELINE_METRIC_UUID_LENGTH);
    }

    TimelineMetricMetadataKey key = uuidKeyMap.get(new String(metricUuid));
    return key != null ? key.getMetricName() : null;
  }

  public TimelineMetric getMetricFromUuid(byte[] uuid) {
    if (uuid == null) {
      return null;
    }

    if (uuid.length == TIMELINE_METRIC_UUID_LENGTH) {
      TimelineMetricMetadataKey key = uuidKeyMap.get(new String(uuid));
      return key != null ? new TimelineMetric(key.metricName, null, key.appId, key.instanceId) : null;
    } else {
      byte[] metricUuid = ArrayUtils.subarray(uuid, 0, TIMELINE_METRIC_UUID_LENGTH);
      TimelineMetricMetadataKey key = uuidKeyMap.get(new String(metricUuid));
      if (key == null) {
        LOG.error("TimelineMetricMetadataKey is null for : " + Arrays.toString(uuid));
        return null;
      }
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(key.metricName);
      timelineMetric.setAppId(key.appId);
      timelineMetric.setInstanceId(key.instanceId);

      byte[] hostUuid = ArrayUtils.subarray(uuid, TIMELINE_METRIC_UUID_LENGTH, HOSTNAME_UUID_LENGTH + TIMELINE_METRIC_UUID_LENGTH);
      timelineMetric.setHostName(uuidHostMap.get(new String(hostUuid)));
      return timelineMetric;
    }
  }

  /**
   * Returns the set of UUIDs for a given GET request. If there are wildcards (%), resolves them based on UUID map.
   * @param metricNames
   * @param hostnames
   * @param appId
   * @param instanceId
   * @return Set of UUIds
   */
  public List<byte[]> getUuids(Collection<String> metricNames, List<String> hostnames, String appId, String instanceId) {

    Collection<String> sanitizedMetricNames = new HashSet<>();

    for (String metricName : metricNames) {
      if (metricName.contains("%")) {
        String metricRegEx;
        //Special case handling for metric name with * and __%.
        //For example, dfs.NNTopUserOpCounts.windowMs=300000.op=*.user=%.count
        // or dfs.NNTopUserOpCounts.windowMs=300000.op=__%.user=%.count
        if (metricName.contains("*") || metricName.contains("__%")) {
          String metricNameWithEscSeq = metricName.replace("*", "\\*").replace("__%", "..%");
          metricRegEx = metricNameWithEscSeq.replace("%", ".*");
        } else {
          metricRegEx = metricName.replace("%", ".*");
        }
        for (TimelineMetricMetadataKey key : METADATA_CACHE.keySet()) {
          String metricNameFromMetadata = key.getMetricName();
          if (metricNameFromMetadata.matches(metricRegEx)) {
            sanitizedMetricNames.add(metricNameFromMetadata);
          }
        }
      } else {
        sanitizedMetricNames.add(metricName);
      }
    }

    Set<String> sanitizedHostNames = new HashSet<>();
    if (CollectionUtils.isNotEmpty(hostnames)) {
      for (String hostname : hostnames) {
        if (hostname.contains("%")) {
          String hostRegEx;
          hostRegEx = hostname.replace("%", ".*");
          for (String host : HOSTED_APPS_MAP.keySet()) {
            if (host.matches(hostRegEx)) {
              sanitizedHostNames.add(host);
            }
          }
        } else {
          sanitizedHostNames.add(hostname);
        }
      }
    }

    List<byte[]> uuids = new ArrayList<>();

    if (!(appId.equals("HOST") || appId.equals("FLUME_HANDLER"))) { //HACK.. Why??
      appId = appId.toLowerCase();
    }
    if (CollectionUtils.isNotEmpty(sanitizedHostNames)) {
      if (CollectionUtils.isNotEmpty(sanitizedMetricNames)) {
        for (String metricName : sanitizedMetricNames) {
          TimelineMetric metric = new TimelineMetric();
          metric.setMetricName(metricName);
          metric.setAppId(appId);
          metric.setInstanceId(instanceId);
          for (String hostname : sanitizedHostNames) {
            metric.setHostName(hostname);
            byte[] uuid = getUuid(metric);
            if (uuid != null) {
              uuids.add(uuid);
            }
          }
        }
      } else {
        for (String hostname : sanitizedHostNames) {
          byte[] uuid = getUuidForHostname(hostname);
          if (uuid != null) {
            uuids.add(uuid);
          }
        }
      }
    } else {
      for (String metricName : sanitizedMetricNames) {
        TimelineClusterMetric metric = new TimelineClusterMetric(metricName, appId, instanceId, -1l);
        byte[] uuid = getUuid(metric);
        if (uuid != null) {
          uuids.add(uuid);
        }
      }
    }

    return uuids;
  }

  public Map<String, TimelineMetricMetadataKey> getUuidKeyMap() {
    return uuidKeyMap;
  }

  public List<String> getNotLikeHostnames(List<String> hostnames) {
    List<String> result = new ArrayList<>();
    Set<String> sanitizedHostNames = new HashSet<>();
    if (CollectionUtils.isNotEmpty(hostnames)) {
      for (String hostname : hostnames) {
        if (hostname.contains("%")) {
          String hostRegEx;
          hostRegEx = hostname.replace("%", ".*");
          for (String host : HOSTED_APPS_MAP.keySet()) {
            if (host.matches(hostRegEx)) {
              sanitizedHostNames.add(host);
            }
          }
        } else {
          sanitizedHostNames.add(hostname);
        }
      }
    }

    for (String hostname: HOSTED_APPS_MAP.keySet()) {
      if (!sanitizedHostNames.contains(hostname)) {
        result.add(hostname);
      }
    }
    return result;
  }
}
