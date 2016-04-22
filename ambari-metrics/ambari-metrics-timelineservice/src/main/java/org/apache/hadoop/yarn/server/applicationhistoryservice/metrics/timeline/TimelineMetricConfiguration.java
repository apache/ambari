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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Configuration class that reads properties from ams-site.xml. All values
 * for time or intervals are given in seconds.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class TimelineMetricConfiguration {
  private static final Log LOG = LogFactory.getLog(TimelineMetricConfiguration.class);

  public static final String HBASE_SITE_CONFIGURATION_FILE = "hbase-site.xml";
  public static final String METRICS_SITE_CONFIGURATION_FILE = "ams-site.xml";

  public static final String TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR =
    "timeline.metrics.aggregator.checkpoint.dir";

  public static final String DEFAULT_CHECKPOINT_LOCATION =
    System.getProperty("java.io.tmpdir");

  public static final String HBASE_ENCODING_SCHEME =
    "timeline.metrics.hbase.data.block.encoding";

  public static final String HBASE_COMPRESSION_SCHEME =
    "timeline.metrics.hbase.compression.scheme";

  public static final String PRECISION_TABLE_TTL =
    "timeline.metrics.host.aggregator.ttl";

  public static final String HOST_MINUTE_TABLE_TTL =
    "timeline.metrics.host.aggregator.minute.ttl";

  public static final String HOST_DAILY_TABLE_TTL =
    "timeline.metrics.host.aggregator.daily.ttl";

  public static final String HOST_HOUR_TABLE_TTL =
    "timeline.metrics.host.aggregator.hourly.ttl";

  public static final String CLUSTER_SECOND_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.second.ttl";

  public static final String CLUSTER_MINUTE_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.minute.ttl";

  public static final String CLUSTER_HOUR_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.hourly.ttl";

  public static final String CLUSTER_DAILY_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.daily.ttl";

  public static final String CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL =
    "timeline.metrics.cluster.aggregator.second.timeslice.interval";

  public static final String AGGREGATOR_CHECKPOINT_DELAY =
    "timeline.metrics.service.checkpointDelay";

  public static final String RESULTSET_FETCH_SIZE =
    "timeline.metrics.service.resultset.fetchSize";

  public static final String HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL =
    "timeline.metrics.host.aggregator.minute.interval";

  public static final String HOST_AGGREGATOR_HOUR_SLEEP_INTERVAL =
    "timeline.metrics.host.aggregator.hourly.interval";

  public static final String HOST_AGGREGATOR_DAILY_SLEEP_INTERVAL =
    "timeline.metrics.host.aggregator.daily.interval";

  public static final String CLUSTER_AGGREGATOR_SECOND_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.second.interval";

  public static final String CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.minute.interval";

  public static final String CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.hourly.interval";

  public static final String CLUSTER_AGGREGATOR_DAILY_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.daily.interval";

  public static final String HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.host.aggregator.minute.checkpointCutOffMultiplier";

  public static final String HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.host.aggregator.hourly.checkpointCutOffMultiplier";

  public static final String HOST_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_SECOND_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.second.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.minute.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.hourly.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier";

  public static final String GLOBAL_RESULT_LIMIT =
    "timeline.metrics.service.default.result.limit";

  public static final String GLOBAL_MAX_RETRIES =
    "timeline.metrics.service.default.max_retries";

  public static final String GLOBAL_RETRY_INTERVAL =
    "timeline.metrics.service.default.retryInterval";

  public static final String HOST_AGGREGATOR_MINUTE_DISABLED =
    "timeline.metrics.host.aggregator.minute.disabled";

  public static final String HOST_AGGREGATOR_HOUR_DISABLED =
    "timeline.metrics.host.aggregator.hourly.disabled";

  public static final String HOST_AGGREGATOR_DAILY_DISABLED =
    "timeline.metrics.host.aggregator.hourly.disabled";

  public static final String CLUSTER_AGGREGATOR_SECOND_DISABLED =
    "timeline.metrics.cluster.aggregator.second.disabled";

  public static final String CLUSTER_AGGREGATOR_MINUTE_DISABLED =
    "timeline.metrics.cluster.aggregator.minute.disabled";

  public static final String CLUSTER_AGGREGATOR_HOUR_DISABLED =
    "timeline.metrics.cluster.aggregator.hourly.disabled";

  public static final String CLUSTER_AGGREGATOR_DAILY_DISABLED =
    "timeline.metrics.cluster.aggregator.daily.disabled";

  public static final String DISABLE_APPLICATION_TIMELINE_STORE =
    "timeline.service.disable.application.timeline.store";

  public static final String WEBAPP_HTTP_ADDRESS =
    "timeline.metrics.service.webapp.address";

  public static final String TIMELINE_SERVICE_RPC_ADDRESS =
    "timeline.metrics.service.rpc.address";

  public static final String CLUSTER_AGGREGATOR_APP_IDS =
    "timeline.metrics.service.cluster.aggregator.appIds";

  public static final String SERVER_SIDE_TIMESIFT_ADJUSTMENT =
    "timeline.metrics.service.cluster.aggregator.timeshift.adjustment";

  public static final String OUT_OFF_BAND_DATA_TIME_ALLOWANCE =
    "timeline.metrics.service.outofband.time.allowance.millis";

  public static final String USE_GROUPBY_AGGREGATOR_QUERIES =
    "timeline.metrics.service.use.groupBy.aggregators";

  public static final String HANDLER_THREAD_COUNT =
    "timeline.metrics.service.handler.thread.count";

  public static final String WATCHER_DISABLED =
    "timeline.metrics.service.watcher.disabled";

  public static final String WATCHER_INITIAL_DELAY =
    "timeline.metrics.service.watcher.initial.delay";

  public static final String WATCHER_DELAY =
    "timeline.metrics.service.watcher.delay";

  public static final String WATCHER_TIMEOUT =
    "timeline.metrics.service.watcher.timeout";

  public static final String WATCHER_MAX_FAILURES =
    "timeline.metrics.service.watcher.max.failures";

  public static final String PRECISION_TABLE_SPLIT_POINTS =
    "timeline.metrics.host.aggregate.splitpoints";

  public static final String AGGREGATE_TABLE_SPLIT_POINTS =
    "timeline.metrics.cluster.aggregate.splitpoints";

  public static final String TIMELINE_SERVICE_HTTP_POLICY =
    "timeline.metrics.service.http.policy";

  public static final String DISABLE_METRIC_METADATA_MGMT =
    "timeline.metrics.service.metadata.management.disabled";

  public static final String METRICS_METADATA_SYNC_INIT_DELAY =
    "timeline.metrics.service.metadata.sync.init.delay";

  public static final String METRICS_METADATA_SYNC_SCHEDULE_DELAY =
    "timeline.metrics.service.metadata.sync.delay";

  public static final String TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED =
    "timeline.metrics.cluster.aggregator.interpolation.enabled";

  public static final String TIMELINE_METRICS_TABLES_DURABILITY =
    "timeline.metrics.tables.durability";

  public static final String HBASE_BLOCKING_STORE_FILES =
    "hbase.hstore.blockingStoreFiles";

  public static final String HOST_APP_ID = "HOST";

  private Configuration hbaseConf;
  private Configuration metricsConf;
  private volatile boolean isInitialized = false;

  public void initialize() throws URISyntaxException, MalformedURLException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = getClass().getClassLoader();
    }
    URL hbaseResUrl = classLoader.getResource(HBASE_SITE_CONFIGURATION_FILE);
    URL amsResUrl = classLoader.getResource(METRICS_SITE_CONFIGURATION_FILE);
    LOG.info("Found hbase site configuration: " + hbaseResUrl);
    LOG.info("Found metric service configuration: " + amsResUrl);

    if (hbaseResUrl == null) {
      throw new IllegalStateException("Unable to initialize the metrics " +
        "subsystem. No hbase-site present in the classpath.");
    }

    if (amsResUrl == null) {
      throw new IllegalStateException("Unable to initialize the metrics " +
        "subsystem. No ams-site present in the classpath.");
    }

    hbaseConf = new Configuration(true);
    hbaseConf.addResource(hbaseResUrl.toURI().toURL());
    metricsConf = new Configuration(true);
    metricsConf.addResource(amsResUrl.toURI().toURL());
    isInitialized = true;
  }

  public Configuration getHbaseConf() throws URISyntaxException, MalformedURLException {
    if (!isInitialized) {
      initialize();
    }
    return hbaseConf;
  }

  public Configuration getMetricsConf() throws URISyntaxException, MalformedURLException {
    if (!isInitialized) {
      initialize();
    }
    return metricsConf;
  }

  public String getWebappAddress() {
    String defaultHttpAddress = "0.0.0.0:6188";
    if (metricsConf != null) {
      return metricsConf.get(WEBAPP_HTTP_ADDRESS, defaultHttpAddress);
    }
    return defaultHttpAddress;
  }

  public int getTimelineMetricsServiceHandlerThreadCount() {
    if (metricsConf != null) {
      return Integer.parseInt(metricsConf.get(HANDLER_THREAD_COUNT, "20"));
    }
    return 20;
  }

  public boolean isTimelineMetricsServiceWatcherDisabled() {
    if (metricsConf != null) {
      return Boolean.parseBoolean(metricsConf.get(WATCHER_DISABLED, "false"));
    }
    return false;
  }

  public int getTimelineMetricsServiceWatcherInitDelay() {
    if (metricsConf != null) {
      return Integer.parseInt(metricsConf.get(WATCHER_INITIAL_DELAY, "600"));
    }
    return 600;
  }

  public int getTimelineMetricsServiceWatcherDelay() {
    if (metricsConf != null) {
      return Integer.parseInt(metricsConf.get(WATCHER_DELAY, "30"));
    }
    return 30;
  }

  public int getTimelineMetricsServiceWatcherTimeout() {
    if (metricsConf != null) {
      return Integer.parseInt(metricsConf.get(WATCHER_TIMEOUT, "30"));
    }
    return 30;
  }

  public int getTimelineMetricsServiceWatcherMaxFailures() {
    if (metricsConf != null) {
      return Integer.parseInt(metricsConf.get(WATCHER_MAX_FAILURES, "3"));
    }
    return 3;
  }

  public String getTimelineServiceRpcAddress() {
    String defaultRpcAddress = "0.0.0.0:60200";
    if (metricsConf != null) {
      return metricsConf.get(TIMELINE_SERVICE_RPC_ADDRESS, defaultRpcAddress);
    }
    return defaultRpcAddress;
  }
}
