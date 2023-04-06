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
package org.apache.ambari.metrics.core.timeline.discovery;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.metrics.core.timeline.MetricsSystemInitializationException;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.uuid.MetricUuidGenStrategy;
import org.apache.ambari.metrics.core.timeline.uuid.MD5UuidGenStrategy;
import org.apache.ambari.metrics.core.timeline.uuid.Murmur3HashUuidGenStrategy;
import org.apache.ambari.metrics.core.timeline.uuid.TimelineMetricUuid;
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
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HBASE_ENCODING_SCHEME;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TRANSIENT_METRIC_PATTERNS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_INIT_DELAY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.METRICS_METADATA_SYNC_SCHEDULE_DELAY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_UUID_GEN_STRATEGY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_METADATA_FILTERS;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_HOSTED_APPS_METADATA_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_INSTANCE_HOST_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_METRICS_METADATA_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.DEFAULT_ENCODING;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.DEFAULT_TABLE_COMPRESSION;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaMetricPatterns;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaRegexFromSqlRegex;

public class TimelineMetricMetadataManager {
  private static final Log LOG = LogFactory.getLog(TimelineMetricMetadataManager.class);
  // Cache all metadata on retrieval
  private final Map<TimelineMetricMetadataKey, TimelineMetricMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
  private final Map<TimelineMetricUuid, TimelineMetricMetadataKey> uuidKeyMap = new ConcurrentHashMap<>();
  // Map to lookup apps on a host
  private final Map<String, TimelineMetricHostMetadata> HOSTED_APPS_MAP = new ConcurrentHashMap<>();
  private final Map<TimelineMetricUuid, String> uuidHostMap = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> INSTANCE_HOST_MAP = new ConcurrentHashMap<>();
  // Sync only when needed
  AtomicBoolean SYNC_HOSTED_APPS_METADATA = new AtomicBoolean(false);
  AtomicBoolean SYNC_HOSTED_INSTANCES_METADATA = new AtomicBoolean(false);

  private MetricUuidGenStrategy uuidGenStrategy = new Murmur3HashUuidGenStrategy();
  public static final int TIMELINE_METRIC_UUID_LENGTH = 16;
  public static int HOSTNAME_UUID_LENGTH = 4;

  //Transient metric patterns. No UUID management and aggregation for such metrics.
  private List<String> transientMetricPatterns = new ArrayList<>();

  // Single thread to sync back new writes to the store
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private PhoenixHBaseAccessor hBaseAccessor;
  private Configuration metricsConf;

  TimelineMetricMetadataSync metricMetadataSync;
  // Filter metrics names matching given patterns, from metadata
  private final List<String> metricNameFilters = new ArrayList<>();

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

    String transientMetricPatternsString = metricsConf.get(TRANSIENT_METRIC_PATTERNS, StringUtils.EMPTY);
    if (StringUtils.isNotEmpty(transientMetricPatternsString)) {
      LOG.info("Skipping UUID for patterns : " + transientMetricPatternsString);
      transientMetricPatterns.addAll(getJavaMetricPatterns(transientMetricPatternsString));
    }
  }

  public TimelineMetricMetadataManager(PhoenixHBaseAccessor hBaseAccessor) throws MalformedURLException, URISyntaxException {
    this(TimelineMetricConfiguration.getInstance().getMetricsConf(), hBaseAccessor);
  }

  /**
   * Initialize Metadata from the store
   */
  public void initializeMetadata() {
    initializeMetadata(true);
  }

  /**
   * Initialize Metadata from the store
   */
  public void initializeMetadata(boolean scheduleMetadateSync) {

    //Create metadata schema
    Connection conn = null;
    Statement stmt = null;

    String encoding = metricsConf.get(HBASE_ENCODING_SCHEME, DEFAULT_ENCODING);
    String compression = metricsConf.get(HBASE_COMPRESSION_SCHEME, DEFAULT_TABLE_COMPRESSION);

    try {
      LOG.info("Initializing metrics metadata schema...");
      conn = hBaseAccessor.getConnectionRetryingOnException();
      stmt = conn.createStatement();

      // Metadata
      String metadataSql = String.format(CREATE_METRICS_METADATA_TABLE_SQL,
        encoding, compression);
      stmt.executeUpdate(metadataSql);

      String hostedAppSql = String.format(CREATE_HOSTED_APPS_METADATA_TABLE_SQL,
        encoding, compression);
      stmt.executeUpdate(hostedAppSql);

      //Host Instances table
      String hostedInstancesSql = String.format(CREATE_INSTANCE_HOST_TABLE_SQL,
        encoding, compression);
      stmt.executeUpdate(hostedInstancesSql);
    } catch (SQLException | InterruptedException sql) {
      LOG.error("Error creating Metrics Schema in HBase using Phoenix.", sql);
      throw new MetricsSystemInitializationException(
        "Error creating Metrics Metadata Schema in HBase using Phoenix.", sql);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
    }

    metricMetadataSync = new TimelineMetricMetadataSync(this);
    // Schedule the executor to sync to store
    if (scheduleMetadateSync) {
      executorService.scheduleWithFixedDelay(metricMetadataSync,
        metricsConf.getInt(METRICS_METADATA_SYNC_INIT_DELAY, 120), // 2 minutes
        metricsConf.getInt(METRICS_METADATA_SYNC_SCHEDULE_DELAY, 300), // 5 minutes
        TimeUnit.SECONDS);
    }
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
        TimelineMetricHostMetadata newHostMetadata = new TimelineMetricHostMetadata(apps);
        HOSTED_APPS_MAP.put(hostname, newHostMetadata);
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

  public TimelineMetricMetadata createTimelineMetricMetadata(TimelineMetric timelineMetric, boolean isWhitelisted) {
    TimelineMetricMetadata timelineMetricMetadata = new TimelineMetricMetadata(
      timelineMetric.getMetricName(),
      timelineMetric.getAppId(),
      timelineMetric.getInstanceId(),
      timelineMetric.getUnits(),
      timelineMetric.getType(),
      timelineMetric.getStartTime(),
      supportAggregates(timelineMetric),
      isWhitelisted
    );

    //Set UUID for metadata on the write path. Do not pass in hostname here since we only want Metric metadata, not host metadata.
    if (!isTransientMetric(timelineMetric.getMetricName(), timelineMetric.getAppId())) {
      byte[] uuid = getUuid(timelineMetric.getMetricName(), timelineMetric.getAppId(), timelineMetric.getInstanceId(), null, true);
      timelineMetricMetadata.setUuid(uuid);
    }
    return timelineMetricMetadata;
  }

  boolean isDistributedModeEnabled() {
    String mode = metricsConf.get("timeline.metrics.service.operation.mode");
    return (mode != null) && mode.equals("distributed");
  }

  /**
   * Fetch metrics metadata from store from V1 table (no UUID)
   * @throws SQLException
   */
  Map<TimelineMetricMetadataKey, TimelineMetricMetadata> getMetadataFromStoreV1() throws SQLException {
    return hBaseAccessor.getTimelineMetricMetadataV1();
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
        uuidKeyMap.put(new TimelineMetricUuid(timelineMetricMetadata.getUuid()), key);
      }
    }

    for (String host : HOSTED_APPS_MAP.keySet()) {
      TimelineMetricHostMetadata timelineMetricHostMetadata = HOSTED_APPS_MAP.get(host);
      if (timelineMetricHostMetadata != null && timelineMetricHostMetadata.getUuid() != null) {
        uuidHostMap.put(new TimelineMetricUuid(timelineMetricHostMetadata.getUuid()), host);
      }
    }
  }

  /**
   * Returns the UUID gen strategy.
   * @param configuration the config
   * @return the UUID generator of type org.apache.ambari.metrics.core.timeline.uuid.MetricUuidGenStrategy
   */
  private MetricUuidGenStrategy getUuidStrategy(Configuration configuration) {
    return new Murmur3HashUuidGenStrategy();
  }

  /**
   * Given the hostname, generates a byte array of length 'HOSTNAME_UUID_LENGTH'
   * @param hostname the hostname
   * @param createIfNotPresent Generate UUID if not present.
   * @return uuid byte array of length 'HOSTNAME_UUID_LENGTH'
   */
  private byte[] getUuidForHostname(String hostname, boolean createIfNotPresent) {

    TimelineMetricHostMetadata timelineMetricHostMetadata = HOSTED_APPS_MAP.get(hostname);
    if (timelineMetricHostMetadata != null) {
      byte[] uuid = timelineMetricHostMetadata.getUuid();
      if (uuid != null) {
        return uuid;
      }
    }

    if (!createIfNotPresent) {
      LOG.debug("UUID not found for " + hostname + ", createIfNotPresent is false");
      return null;
    }

    byte[] uuid = uuidGenStrategy.computeUuid(hostname, HOSTNAME_UUID_LENGTH);
    TimelineMetricUuid timelineMetricUuid = new TimelineMetricUuid(uuid);
    if (uuidHostMap.containsKey(timelineMetricUuid) && !hostname.equals(uuidHostMap.get(timelineMetricUuid))) {
      LOG.error("Duplicate key computed for " + hostname +", Collides with  " + uuidHostMap.get(timelineMetricUuid));
      return null;
    }

    timelineMetricHostMetadata = HOSTED_APPS_MAP.computeIfAbsent(hostname, k -> new TimelineMetricHostMetadata());
    if (timelineMetricHostMetadata.getUuid() == null) {
      timelineMetricHostMetadata.setUuid(uuid);
    }
    if (!uuidHostMap.containsKey(timelineMetricUuid)) {
      uuidHostMap.put(timelineMetricUuid, hostname);
    }

    return uuid;
  }

  /**
   * Given a timelineClusterMetric instance, generates a UUID for Metric-App-Instance combination.
   * @param timelineClusterMetric The timeline cluster metric for which the UUID needs to be generated.
   * @param createIfNotPresent Generate UUID if not present.
   * @return uuid byte array of length 'TIMELINE_METRIC_UUID_LENGTH'
   */
  public byte[] getUuid(TimelineClusterMetric timelineClusterMetric, boolean createIfNotPresent) {

    TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(timelineClusterMetric.getMetricName(),
      timelineClusterMetric.getAppId(), timelineClusterMetric.getInstanceId());

    TimelineMetricMetadata timelineMetricMetadata = METADATA_CACHE.get(key);
    if (timelineMetricMetadata != null) {
      byte[] uuid = timelineMetricMetadata.getUuid();
      if (uuid != null) {
        return uuid;
      }
    }

    if (!createIfNotPresent) {
      LOG.debug("UUID not found for " + key + ", createIfNotPresent is false");
      return null;
    }

    byte[] uuidBytes = uuidGenStrategy.computeUuid(timelineClusterMetric, TIMELINE_METRIC_UUID_LENGTH);

    TimelineMetricUuid uuid = new TimelineMetricUuid(uuidBytes);
    if (uuidKeyMap.containsKey(uuid) && !uuidKeyMap.get(uuid).equals(key)) {
      TimelineMetricMetadataKey collidingKey = uuidKeyMap.get(uuid);
      LOG.error("Duplicate key " + uuid + " computed for " + timelineClusterMetric + ", Collides with  " + collidingKey);
      return null;
    }

    timelineMetricMetadata = METADATA_CACHE.get(key);
    if (timelineMetricMetadata == null) {
      timelineMetricMetadata = new TimelineMetricMetadata();
      timelineMetricMetadata.setMetricName(timelineClusterMetric.getMetricName());
      timelineMetricMetadata.setAppId(timelineClusterMetric.getAppId());
      timelineMetricMetadata.setInstanceId(timelineClusterMetric.getInstanceId());
      METADATA_CACHE.put(key, timelineMetricMetadata);
    }

    if (timelineMetricMetadata.getUuid() == null) {
      timelineMetricMetadata.setUuid(uuid.uuid);
    }
    timelineMetricMetadata.setIsPersisted(false);

    if (!uuidKeyMap.containsKey(uuid)) {
      uuidKeyMap.put(uuid, key);
    }
    return uuid.uuid;
  }

  /**
   * Given a timelineMetric instance, generates a UUID for Metric-App-Instance-Host combination.
   * @param timelineMetric The timeline metric for which the UUID needs to be generated.
   * @param createIfNotPresent Generate UUID if not present.
   * @return uuid byte array of length 'TIMELINE_METRIC_UUID_LENGTH' + 'HOSTNAME_UUID_LENGTH'
   */
  public byte[] getUuid(TimelineMetric timelineMetric, boolean createIfNotPresent) {

    byte[] metricUuid = getUuid(new TimelineClusterMetric(timelineMetric.getMetricName(), timelineMetric.getAppId(),
      timelineMetric.getInstanceId(), -1l), createIfNotPresent);
    byte[] hostUuid = getUuidForHostname(timelineMetric.getHostName(), createIfNotPresent);

    if (hostUuid != null) {
      putIfModifiedHostedAppsMetadata(timelineMetric.getHostName(), timelineMetric.getAppId());
    }

    if (metricUuid == null || hostUuid == null) {
      return null;
    }
    return ArrayUtils.addAll(metricUuid, hostUuid);
  }

  /**
   * Given a metric name, appId, instanceId and hotname, generates a UUID for Metric-App-Instance-Host combination.
   * @param createIfNotPresent Generate UUID if not present.
   * @return uuid byte array of length 'TIMELINE_METRIC_UUID_LENGTH' + 'HOSTNAME_UUID_LENGTH'
   */
  public byte[] getUuid(String metricName, String appId, String instanceId, String hostname, boolean createIfNotPresent) {

    byte[] metricUuid = getUuid(new TimelineClusterMetric(metricName, appId, instanceId, -1l), createIfNotPresent);
    if (StringUtils.isNotEmpty(hostname)) {
      byte[] hostUuid = getUuidForHostname(hostname, createIfNotPresent);
      if (hostUuid == null || metricUuid == null) {
        return null;
      }
      return ArrayUtils.addAll(metricUuid, hostUuid);
    }
    return metricUuid;
  }

  public String getMetricNameFromUuid(byte[] uuid) {

    byte[] metricUuid = uuid;
    if (uuid.length == TIMELINE_METRIC_UUID_LENGTH + HOSTNAME_UUID_LENGTH) {
      metricUuid = ArrayUtils.subarray(uuid, 0, TIMELINE_METRIC_UUID_LENGTH);
    }

    TimelineMetricMetadataKey key = uuidKeyMap.get(new TimelineMetricUuid(metricUuid));
    return key != null ? key.getMetricName() : null;
  }

  /**
   * Given a UUID (from DB hopefully), return the timeline metric it is associated with.
   * @param uuid 'TIMELINE_METRIC_UUID_LENGTH' + 'HOSTNAME_UUID_LENGTH' byte UUID.
   * @return TimelineMetric object if present in the metadata.
   */
  public TimelineMetric getMetricFromUuid(byte[] uuid) {
    if (uuid == null) {
      return null;
    }

    if (uuid.length == TIMELINE_METRIC_UUID_LENGTH) {
      TimelineMetricMetadataKey key = uuidKeyMap.get(new TimelineMetricUuid(uuid));
      return key != null ? new TimelineMetric(key.metricName, null, key.appId, key.instanceId) : null;
    } else {
      byte[] metricUuid = ArrayUtils.subarray(uuid, 0, TIMELINE_METRIC_UUID_LENGTH);
      TimelineMetricMetadataKey key = uuidKeyMap.get(new TimelineMetricUuid(metricUuid));
      if (key == null) {
        LOG.error("TimelineMetricMetadataKey is null for : " + Arrays.toString(uuid));
        return null;
      }
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(key.metricName);
      timelineMetric.setAppId(key.appId);
      timelineMetric.setInstanceId(key.instanceId);

      byte[] hostUuid = ArrayUtils.subarray(uuid, TIMELINE_METRIC_UUID_LENGTH, HOSTNAME_UUID_LENGTH + TIMELINE_METRIC_UUID_LENGTH);
      String hostname = uuidHostMap.get(new TimelineMetricUuid(hostUuid));
      if (hostname == null) {
        return null;
      }
      timelineMetric.setHostName(hostname);
      return timelineMetric;
    }
  }

  public List<byte[]> getUuidsForGetMetricQuery(Collection<String> metricNames,
                                                List<String> hostnames,
                                                String appId,
                                                String instanceId) {
    return getUuidsForGetMetricQuery(metricNames, hostnames, appId, instanceId, Collections.EMPTY_LIST);
  }
  /**
   * Returns the set of UUIDs for a given GET request. If there are wildcards (%), resolves them based on UUID map.
   * If metricName-App-Instance or hostname not present in Metadata, the combination will be skipped.
   * @param metricNames
   * @param hostnames
   * @param appId
   * @param instanceId
   * @return Set of UUIds
   */
  public List<byte[]> getUuidsForGetMetricQuery(Collection<String> metricNames,
                                                List<String> hostnames,
                                                String appId,
                                                String instanceId,
                                                List<String> transientMetricNames) {

    Collection<String> sanitizedMetricNames = new HashSet<>();
    List<byte[]> uuids = new ArrayList<>();

    boolean metricNameHasWildcard = false;

    for (String metricName : metricNames) {
      if (hasWildCard(metricName)) {
        metricNameHasWildcard = true;
        break;
      }
    }

    boolean hostNameHasWildcard = false;
    if (CollectionUtils.isNotEmpty(hostnames)) {
      for (String hostname : hostnames) {
        if (hasWildCard(hostname)) {
          hostNameHasWildcard = true;
          break;
        }
      }
    }

    if (hasWildCard(instanceId) || hasWildCard(appId) || hostNameHasWildcard || metricNameHasWildcard) {
      try {
        List<TimelineMetricMetadata> metricMetadataFromStore = hBaseAccessor.scanMetricMetadataForWildCardRequest(metricNames,
          appId, instanceId);
        List<byte[]> hostUuidsFromStore = hBaseAccessor.scanHostMetadataForWildCardRequest(hostnames);

        for (TimelineMetricMetadata matchedEntry : metricMetadataFromStore) {
          if (matchedEntry.getUuid() != null) {
            if (CollectionUtils.isNotEmpty(hostnames)) {
              for (byte[] hostUuidEntry : hostUuidsFromStore) {
                uuids.add(ArrayUtils.addAll(matchedEntry.getUuid(), hostUuidEntry));
              }
            } else {
              uuids.add(matchedEntry.getUuid());
            }
          } else if (isTransientMetric(matchedEntry.getMetricName(), matchedEntry.getAppId())) {
            transientMetricNames.add(matchedEntry.getMetricName());
          }
        }
        return uuids;
      } catch (SQLException e) {
        LOG.error("Unable to query metadata table to check satisfying metric keys for wildcard request : " + e);
        return uuids;
      }
    } else {

      if (CollectionUtils.isNotEmpty(hostnames)) {
        if (CollectionUtils.isNotEmpty(metricNames)) {
          //Skip getting UUID if it is a transient metric.
          //An attempt to get it will also be OK as we don't add null UUIDs.
          for (String metricName : metricNames) {
            if (isTransientMetric(metricName, appId)) {
              transientMetricNames.add(metricName);
              continue;
            }
            TimelineMetric metric = new TimelineMetric();
            metric.setMetricName(metricName);
            metric.setAppId(appId);
            metric.setInstanceId(instanceId);
            for (String hostname : hostnames) {
              metric.setHostName(hostname);
              byte[] uuid = getUuid(metric, false);
              if (uuid != null) {
                uuids.add(uuid);
              }
            }
          }
        } else {
          for (String hostname : hostnames) {
            byte[] uuid = getUuidForHostname(hostname, false);
            if (uuid != null) {
              uuids.add(uuid);
            }
          }
        }
      } else {
        for (String metricName : metricNames) {
          //Skip getting UUID if it is a transient metric. An attempt to get it will also be OK as we don't add null UUIDs.
          if (isTransientMetric(metricName, appId)) {
            transientMetricNames.add(metricName);
            continue;
          }
          TimelineClusterMetric metric = new TimelineClusterMetric(metricName, appId, instanceId, -1l);
          byte[] uuid = getUuid(metric, false);
          if (uuid != null) {
            uuids.add(uuid);
          }
        }
      }
    }

    return uuids;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public List<String> getNotLikeHostnames(List<String> hostnames) {
    List<String> result = new ArrayList<>();
    Set<String> sanitizedHostNames = getSanitizedHostnames(hostnames);
    for (String hostname: HOSTED_APPS_MAP.keySet()) {
      if (!sanitizedHostNames.contains(hostname)) {
        result.add(hostname);
      }
    }
    return result;
  }

  private Set<String> getSanitizedHostnames(List<String> hostnamedWithOrWithoutWildcard) {

    Set<String> sanitizedHostNames = new HashSet<>();
    if (CollectionUtils.isNotEmpty(hostnamedWithOrWithoutWildcard)) {
      for (String hostname : hostnamedWithOrWithoutWildcard) {
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
    return sanitizedHostNames;
  }

  /**
   *
   * @param appId
   * @param metricPattern
   * @param includeBlacklistedMetrics
   * @return
   * @throws SQLException
   * @throws IOException
   */
  public Map<String, List<TimelineMetricMetadata>> getTimelineMetricMetadataByAppId(String appId, String metricPattern,
                                                                                    boolean includeBlacklistedMetrics) throws SQLException, IOException {

    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata = getMetadataCache();

    boolean filterByAppId = StringUtils.isNotEmpty(appId);
    boolean filterByMetricName = StringUtils.isNotEmpty(metricPattern);
    Pattern metricFilterPattern = null;
    if (filterByMetricName) {
      metricFilterPattern = Pattern.compile(metricPattern);
    }

    // Group Metadata by AppId
    Map<String, List<TimelineMetricMetadata>> metadataByAppId = new HashMap<>();
    for (TimelineMetricMetadata metricMetadata : metadata.values()) {

      if (!includeBlacklistedMetrics && !metricMetadata.isWhitelisted()) {
        continue;
      }

      String currentAppId = metricMetadata.getAppId();
      if (filterByAppId && !currentAppId.equals(appId)) {
        continue;
      }

      if (filterByMetricName) {
        Matcher m = metricFilterPattern.matcher(metricMetadata.getMetricName());
        if (!m.find()) {
          continue;
        }
      }

      List<TimelineMetricMetadata> metadataList = metadataByAppId.get(currentAppId);
      if (metadataList == null) {
        metadataList = new ArrayList<>();
        metadataByAppId.put(currentAppId, metadataList);
      }

      metadataList.add(metricMetadata);
    }

    return metadataByAppId;
  }

  /**
   * Returns metadata summary
   * @return
   * @throws IOException
   * @throws SQLException
   */
  public Map<String, String> getMetadataSummary() throws IOException, SQLException {
    Map<String, String> summary = new HashMap<>();
    summary.put("Number of Hosts", String.valueOf(HOSTED_APPS_MAP.size()));
    Map<String, List<TimelineMetricMetadata>> metadataMap = getTimelineMetricMetadataByAppId(StringUtils.EMPTY,
      StringUtils.EMPTY,
      true);

    if (metadataMap != null) {
      for (String appId : metadataMap.keySet()) {
        summary.put(appId, String.valueOf(metadataMap.get(appId).size()));
      }
    }
    return summary;
  }


  /**
   *
   * @param metricName
   * @param appId
   * @return
   */
  public boolean isTransientMetric(String metricName, String appId) {
    //Currently we use only metric name. In the future we may use appId as well.

    for (String pattern : transientMetricPatterns) {
      if (metricName.matches(pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Run TimelineMetricMetadataSync once
   */
  public void forceMetricsMetadataSync() {
    if (metricMetadataSync == null) {
      metricMetadataSync = new TimelineMetricMetadataSync(this);
    }
    metricMetadataSync.run();
  }

  private boolean hasWildCard(String key) {
    return StringUtils.isNotEmpty(key) && key.contains("%");
  }

  public void updateMetadataCacheUsingV1Tables() throws SQLException {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadataV1Map = getMetadataFromStoreV1();
    for (TimelineMetricMetadataKey key: METADATA_CACHE.keySet()) {
      TimelineMetricMetadata cacheValue = METADATA_CACHE.get(key);
      TimelineMetricMetadata oldValue = metadataV1Map.get(key);

      if (oldValue != null) {
        if (!cacheValue.isPersisted()) {
          LOG.info(String.format("Updating properties for %s", key));
          cacheValue.setSeriesStartTime(oldValue.getSeriesStartTime());
          cacheValue.setSupportsAggregates(oldValue.isSupportsAggregates());
          cacheValue.setType(oldValue.getType());
          cacheValue.setIsWhitelisted(oldValue.isWhitelisted());
        } else if (oldValue.getSeriesStartTime() < cacheValue.getSeriesStartTime() &&
          cacheValue.getSeriesStartTime() != 0L &&
          cacheValue.isWhitelisted())
        {
          LOG.info(String.format("Updating startTime for %s", key));
          cacheValue.setSeriesStartTime(oldValue.getSeriesStartTime());
          cacheValue.setIsPersisted(false);
        }
      }
    }
  }
}
