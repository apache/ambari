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

  public static final String CLUSTER_MINUTE_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.minute.ttl";

  public static final String CLUSTER_HOUR_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.hourly.ttl";

  public static final String CLUSTER_DAILY_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.daily.ttl";

  public static final String CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL =
    "timeline.metrics.cluster.aggregator.minute.timeslice.interval";

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
    String defaultHttpAddress = "0.0.0.0:8188";
    if (metricsConf != null) {
      return metricsConf.get(WEBAPP_HTTP_ADDRESS, defaultHttpAddress);
    }
    return defaultHttpAddress;
  }

  public String getTimelineServiceRpcAddress() {
    String defaultRpcAddress = "0.0.0.0:60200";
    if (metricsConf != null) {
      return metricsConf.get(TIMELINE_SERVICE_RPC_ADDRESS, defaultRpcAddress);
    }
    return defaultRpcAddress;
  }
}
