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
package org.apache.ambari.metrics.core.timeline.upgrade;

import com.google.common.collect.Sets;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
  private static String patternPrefix = "._p_";
  public static final Map<String, String> CLUSTER_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final Map<String, String> HOST_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final String PROCESSED_METRICS_FILE_NAME = "processed_metrics.txt";
  public static final int DEFAULT_NUMBER_OF_THREADS = 3;
  public static final long ONE_MONTH_MILLIS = 2592000000L;
  public static final long DEFAULT_START_TIME = System.currentTimeMillis() - ONE_MONTH_MILLIS; //Last month

  static {
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_MINUTE_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME);
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_HOURLY_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);
    CLUSTER_AGGREGATE_TABLES_MAPPING.put(METRICS_CLUSTER_AGGREGATE_DAILY_V1_TABLE_NAME, METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME);

    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_MINUTE_V1_TABLE_NAME, METRICS_AGGREGATE_MINUTE_TABLE_NAME);
    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_HOURLY_V1_TABLE_NAME, METRICS_AGGREGATE_HOURLY_TABLE_NAME);
    HOST_AGGREGATE_TABLES_MAPPING.put(METRICS_AGGREGATE_DAILY_V1_TABLE_NAME, METRICS_AGGREGATE_DAILY_TABLE_NAME);
  }

  public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
    String whitelistedFilePath = null;
    Long startTime = null;
    Integer numberOfThreads = null;
    Integer bulkSize = null;

    LOG.info("Initializing system...");

    if (args.length>0) {
      whitelistedFilePath = args[0];
    }
    if (args.length>1) {
      startTime = Long.valueOf(args[1]);
    }
    if (args.length>2) {
      numberOfThreads = Integer.valueOf(args[2]);
    }

    if (startTime == null) {
      startTime = DEFAULT_START_TIME;
    }

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

    Set<String> metricNames = null;
    if (whitelistedFilePath != null) {
      metricNames = readMetricWhitelistFromFile(whitelistedFilePath);
    } else if (timelineMetricConfiguration.isWhitelistingEnabled()) {
      String whitelistFile = timelineMetricConfiguration.getMetricsConf().get(TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE, TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE_LOCATION_DEFAULT);
      metricNames = readMetricWhitelistFromFile(whitelistFile);
    }

    Map<String, Set<String>> processedMetrics = readProcessedMetricsMap();

    FileWriter processedMetricsFileWriter = new FileWriter(PROCESSED_METRICS_FILE_NAME, true);

    LOG.info("Setting up copiers...");
    Set<AbstractPhoenixMetricsCopier> copiers = new HashSet<>();
  for (Map.Entry<String, String> entry : CLUSTER_AGGREGATE_TABLES_MAPPING.entrySet()) {
      //TODO split metric names in batches
      //TODO NOT LIKE for processed metrics
      copiers.add(new PhoenixClusterMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor,
          filterProcessedMetrics(metricNames, processedMetrics, entry.getKey()), startTime, processedMetricsFileWriter));
    }

    for (Map.Entry<String, String> entry : HOST_AGGREGATE_TABLES_MAPPING.entrySet()) {
      //TODO split metric names in batches
      //TODO NOT LIKE for processed metrics
      copiers.add(new PhoenixHostMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor,
          filterProcessedMetrics(metricNames, processedMetrics, entry.getKey()), startTime, processedMetricsFileWriter));
    }

    LOG.info("Running the copy threads...");
    long startTimer = System.currentTimeMillis();
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads == null ? DEFAULT_NUMBER_OF_THREADS : numberOfThreads);
    for (AbstractPhoenixMetricsCopier copier : copiers) {
      executorService.submit(copier);
    }

    executorService.shutdown();

    try {
//      TODO make configurable
      executorService.awaitTermination(300L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    long estimatedTime = System.currentTimeMillis() - startTimer;
    LOG.info(String.format("Copying took %s seconds", estimatedTime/1000.0));

    processedMetricsFileWriter.close();

  }

  private static Set<String> filterProcessedMetrics(Set<String> metricNames, Map<String, Set<String>> processedMetrics, String tableName) {
    if (metricNames == null) {
      return null;
    }
    if (!processedMetrics.containsKey(tableName)) {
      return metricNames;
    }
    return Sets.difference(metricNames, processedMetrics.get(tableName));
  }

  private static Map<String, Set<String>> readProcessedMetricsMap() {
    Map<String, Set<String>> result = new HashMap<>();
    if (!Files.exists(Paths.get(PROCESSED_METRICS_FILE_NAME))) {
      LOG.info(String.format("The processed metrics file %s is missing, assuming there were no metrics processed.", PROCESSED_METRICS_FILE_NAME));
      return new HashMap<>();
    }
    LOG.info(String.format("Reading the list of already copied metrics from %s", PROCESSED_METRICS_FILE_NAME));
    try {
      try (Stream<String> stream = Files.lines(Paths.get(PROCESSED_METRICS_FILE_NAME))) {
        stream.forEach( line -> {
          String [] lineSplit = line.split(":");
          if (!result.containsKey(lineSplit[0])) {
            result.put(lineSplit[0], new HashSet<>(Collections.singletonList(lineSplit[1])));
          } else {
            result.get(lineSplit[0]).add(lineSplit[1]);
          }
        });
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static Set<String> readMetricWhitelistFromFile(String whitelistFile) {
    LOG.info(String.format("Reading metric names from %s", whitelistFile));
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
        //TODO patterns matching + * processing
        if (strLine.startsWith(patternPrefix)) {
          strLine = strLine.replace(patternPrefix, "");
        }
        whitelistedMetrics.add(strLine);
      }
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
    }
    return whitelistedMetrics;
  }
}
