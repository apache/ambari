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

import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_V1_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_V1_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_V1_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_V1_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_V1_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_V1_TABLE_NAME;

public class MetricsSchemaUpgrade {
  private static final Log LOG = LogFactory.getLog(MetricsSchemaUpgrade.class);
  public static Map<String, String> CLUSTER_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static Map<String, String> HOST_AGGREGATE_TABLES_MAPPING = new HashMap<>();

  static {
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_MINUTE_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME);
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_HOURLY_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_DAILY_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME);

    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_MINUTE_V1_TABLE_NAME, METRICS_AGGREGATE_MINUTE_TABLE_NAME);
    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_HOURLY_V1_TABLE_NAME, METRICS_AGGREGATE_HOURLY_TABLE_NAME);
    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_DAILY_V1_TABLE_NAME, METRICS_AGGREGATE_DAILY_TABLE_NAME);
  }

  public static void main(String[] args) throws MalformedURLException, URISyntaxException {
    LOG.info("Initializing system...");
    PhoenixHBaseAccessor hBaseAccessor = new PhoenixHBaseAccessor(null);
    TimelineMetricConfiguration timelineMetricConfiguration = TimelineMetricConfiguration.getInstance();
    timelineMetricConfiguration.initialize();
    //TODO decide if initialize
//    HBaseTimelineMetricsService hBaseTimelineMetricsService = new HBaseTimelineMetricsService(timelineMetricConfiguration);
//    hBaseTimelineMetricsService.init(timelineMetricConfiguration.getMetricsConf());

    TimelineMetricMetadataManager timelineMetricMetadataManager = new TimelineMetricMetadataManager(hBaseAccessor);
    timelineMetricConfiguration.initialize();
    hBaseAccessor.setMetadataInstance(timelineMetricMetadataManager);

    LOG.info("Looking for metric names...");
//    TODO if the file is absent?
//    TODO skip already copied?
//    maybe remove them once copied?
    Set<String> metricNames = new HashSet<>();
    if (timelineMetricConfiguration.isWhitelistingEnabled()) {
      String whitelistFile = timelineMetricConfiguration.getMetricsConf().get(TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE, TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE_LOCATION_DEFAULT);
      metricNames = readMetricWhitelistFromFile(whitelistFile);
    }

    LOG.info("Setting up copiers...");
    Set<AbstractPhoenixMetricsCopier> copiers = new HashSet<>();
    for (Map.Entry<String, String> entry : CLUSTER_AGGREGATE_TABLES_MAPPING.entrySet()) {
      //TODO split metric names in batches
      copiers.add(new PhoenixClusterMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor, metricNames));
    }

    for (Map.Entry<String, String> entry : HOST_AGGREGATE_TABLES_MAPPING.entrySet()) {
      copiers.add(new PhoenixHostMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor, metricNames));
    }

    LOG.info("Running the copy threads...");
    //TODO make configurable
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    for (AbstractPhoenixMetricsCopier copier : copiers) {
      executorService.submit(copier);
    }
    try {
//      TODO make configurable
      executorService.awaitTermination(300L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  private static Set<String> readMetricWhitelistFromFile(String whitelistFile) {
    Set<String> whitelistedMetrics = new HashSet<>();

    BufferedReader br = null;
    String strLine;

    try(FileInputStream fstream = new FileInputStream(whitelistFile)) {
      br = new BufferedReader(new InputStreamReader(fstream));

      while ((strLine = br.readLine()) != null)   {
        strLine = strLine.trim();
        if (StringUtils.isEmpty(strLine)) {
          continue;
        }

        whitelistedMetrics.add(strLine);
      }
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
    }
    //TODO patterns matching
    return whitelistedMetrics;
  }
}
