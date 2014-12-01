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

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * Configuration class that reads properties from ams-site.xml. All values
 * for time or intervals are given in seconds.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public interface TimelineMetricConfiguration {
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
  public static final String HOST_HOUR_TABLE_TTL =
    "timeline.metrics.host.aggregator.hourly.ttl";
  public static final String CLUSTER_MINUTE_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.minute.ttl";
  public static final String CLUSTER_HOUR_TABLE_TTL =
    "timeline.metrics.cluster.aggregator.hourly.ttl";

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

  public static final String CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.minute.interval";

  public static final String CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL =
    "timeline.metrics.cluster.aggregator.hourly.interval";

  public static final String HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.host.aggregator.minute.checkpointCutOffMultiplier";

  public static final String HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.host.aggregator.hourly.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.minute.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER =
    "timeline.metrics.cluster.aggregator.hourly.checkpointCutOffMultiplier";

  public static final String CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_INTERVAL =
    "timeline.metrics.cluster.aggregator.hourly.checkpointCutOffInterval";

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

  public static final String CLUSTER_AGGREGATOR_MINUTE_DISABLED =
    "timeline.metrics.cluster.aggregator.minute.disabled";

  public static final String CLUSTER_AGGREGATOR_HOUR_DISABLED =
    "timeline.metrics.cluster.aggregator.hourly.disabled";

  public static final String DISABLE_APPLICATION_TIMELINE_STORE =
    "timeline.service.disable.application.timeline.store";
}
