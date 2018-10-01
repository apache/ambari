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
package org.apache.ambari.metrics.core.timeline.upgrade.core;

import com.google.common.collect.Iterables;
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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
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

public class MetricsDataMigrationLauncher {
  private static final Log LOG = LogFactory.getLog(MetricsDataMigrationLauncher.class);
  private static final Long DEFAULT_TIMEOUT_MINUTES = 60*24L;
  private static String patternPrefix = "._p_";
  private static final int DEFAULT_BATCH_SIZE = 5;
  public static final Map<String, String> CLUSTER_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final Map<String, String> HOST_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final String DEFAULT_PROCESSED_METRICS_FILE_LOCATION = "/var/log/ambari-metrics-collector/ambari-metrics-migration-state.txt";
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

  private final Set<Set<String>> metricNamesBatches;
  private final String processedMetricsFilePath;
  private Set<String> metricNames;

  private Long startTime;
  private Integer batchSize;
  private Integer numberOfThreads;
  private TimelineMetricConfiguration timelineMetricConfiguration;
  private PhoenixHBaseAccessor hBaseAccessor;
  private TimelineMetricMetadataManager timelineMetricMetadataManager;
  private Map<String, Set<String>> processedMetrics;

  public MetricsDataMigrationLauncher(String whitelistedFilePath, String processedMetricsFilePath, Long startTime, Integer numberOfThreads, Integer batchSize) throws Exception {
    this.startTime = startTime == null? DEFAULT_START_TIME : startTime;
    this.numberOfThreads = numberOfThreads == null? DEFAULT_NUMBER_OF_THREADS : numberOfThreads;
    this.batchSize = batchSize == null? DEFAULT_BATCH_SIZE : batchSize;
    this.processedMetricsFilePath = processedMetricsFilePath == null? DEFAULT_PROCESSED_METRICS_FILE_LOCATION : processedMetricsFilePath;

    initializeHbaseAccessor();

    LOG.info("Looking for whitelisted metric names...");

    if (whitelistedFilePath != null) {
      this.metricNames = readMetricWhitelistFromFile(whitelistedFilePath);
    } else {
      String whitelistFile = timelineMetricConfiguration.getMetricsConf().get(TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE, TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE_LOCATION_DEFAULT);
      metricNames = readMetricWhitelistFromFile(whitelistFile);
    }

    readProcessedMetricsMap();

    LOG.info("Setting up batches...");
    this.metricNamesBatches = new HashSet<>();

    Iterables.partition(metricNames, this.batchSize)
      .forEach(batch -> metricNamesBatches.add(new HashSet<>(batch)));
    LOG.info(String.format("Split metric names into %s batches with size of %s", metricNamesBatches.size(), this.batchSize));
  }


  private void readProcessedMetricsMap() {
    Map<String, Set<String>> result = new HashMap<>();
    if (!Files.exists(Paths.get(processedMetricsFilePath))) {
      LOG.info(String.format("The processed metrics file %s is missing, assuming there were no metrics processed.", processedMetricsFilePath));
      this.processedMetrics = new HashMap<>();
    }
    LOG.info(String.format("Reading the list of already copied metrics from %s", processedMetricsFilePath));
    try {
      try (Stream<String> stream = Files.lines(Paths.get(processedMetricsFilePath))) {
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
      LOG.error(e);
    }
    this.processedMetrics = result;
  }

  public void runMigration(Long timeoutInMinutes) throws IOException {

    FileWriter processedMetricsFileWriter = new FileWriter(processedMetricsFilePath, true);
    LOG.info("Setting up copiers...");
    Set<AbstractPhoenixMetricsCopier> copiers = new HashSet<>();
    for (Set<String> batch : metricNamesBatches) {
      for (Map.Entry<String, String> entry : CLUSTER_AGGREGATE_TABLES_MAPPING.entrySet()) {
        Set<String> filteredMetrics = filterProcessedMetrics(batch, this.processedMetrics, entry.getKey());
        if (!filteredMetrics.isEmpty()) {
          copiers.add(new PhoenixClusterMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor,
            filteredMetrics, startTime, processedMetricsFileWriter));
        }
      }

      for (Map.Entry<String, String> entry : HOST_AGGREGATE_TABLES_MAPPING.entrySet()) {
        Set<String> filteredMetrics = filterProcessedMetrics(batch, processedMetrics, entry.getKey());
        if (!filteredMetrics.isEmpty()) {
          copiers.add(new PhoenixHostMetricsCopier(entry.getKey(), entry.getValue(), hBaseAccessor,
            filteredMetrics, startTime, processedMetricsFileWriter));
        }
      }
    }

    if (copiers.isEmpty()) {
      LOG.info("No copy threads to run, looks like all metrics have been copied.");
      processedMetricsFileWriter.close();
      return;
    }

    LOG.info("Running the copy threads...");
    long startTimer = System.currentTimeMillis();
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads == null ? DEFAULT_NUMBER_OF_THREADS : numberOfThreads);
    for (AbstractPhoenixMetricsCopier copier : copiers) {
      executorService.submit(copier);
    }

    executorService.shutdown();

    try {
      executorService.awaitTermination(timeoutInMinutes, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      LOG.error(e);
    }

    long estimatedTime = System.currentTimeMillis() - startTimer;
    LOG.info(String.format("Copying took %s seconds", estimatedTime/1000.0));

    processedMetricsFileWriter.close();
  }

  private void initializeHbaseAccessor() throws MalformedURLException, URISyntaxException {
    this.hBaseAccessor = new PhoenixHBaseAccessor(null);
    this.timelineMetricConfiguration = TimelineMetricConfiguration.getInstance();
    timelineMetricConfiguration.initialize();

    timelineMetricMetadataManager = new TimelineMetricMetadataManager(hBaseAccessor);
    timelineMetricMetadataManager.initializeMetadata(false);

    hBaseAccessor.setMetadataInstance(timelineMetricMetadataManager);
  }

  private static Set<String> filterProcessedMetrics(Set<String> metricNames, Map<String, Set<String>> processedMetrics, String tableName) {
    if (!processedMetrics.containsKey(tableName)) {
      return metricNames;
    }
    return Sets.difference(metricNames, processedMetrics.get(tableName));
  }

  /**
   * reads metric names from given file
   * replaces all * with % and removes all ._p_
   */
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
        if (strLine.startsWith(patternPrefix)) {
          strLine = strLine.replace(patternPrefix, "");
        }
        if (strLine.contains("*")) {
          strLine = strLine.replaceAll("\\*", "%");
        }
        whitelistedMetrics.add(strLine);
      }
    } catch (IOException ioEx) {
      LOG.error(ioEx);
    }
    return whitelistedMetrics;
  }

  private void saveMetadata() throws SQLException {
    LOG.info("Saving metadata to store...");
    timelineMetricMetadataManager.updateMetadataCacheUsingV1Tables();
    timelineMetricMetadataManager.forceMetricsMetadataSync();
    LOG.info("Metadata was saved.");
  }


  /**
   *
   * @param args
   * REQUIRED args[0] - processedMetricsFilePath - full path to the file where processed metric are/will be stored
   *
   * OPTIONAL args[1] - whitelistedFilePath      - full path to the file with whitelisted metrics filenames
   *                                               if not provided the default whitelist file location will be used if configured
   *                                               if not configured - will result in error
   *          args[2] - startTime                - default value is set to the last 30 days
   *          args[3] - numberOfThreads          - default value is 3
   *          args[4] - batchSize                - default value is 5
   *          args[5] - timeoutInMinutes         - default value is set to the equivalent of 24 hours
   */
  public static void main(String[] args) {
    String processedMetricsFilePath = null;
    String whitelistedFilePath = null;
    Long startTime = null;
    Integer numberOfThreads = null;
    Integer batchSize = null;
    Long timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

    if (args.length>0) {
      processedMetricsFilePath = args[0];
    }
    if (args.length>1) {
      whitelistedFilePath = args[1];
    }
    if (args.length>2) {
      startTime = Long.valueOf(args[2]);
    }
    if (args.length>3) {
      numberOfThreads = Integer.valueOf(args[3]);
    }
    if (args.length>4) {
      batchSize = Integer.valueOf(args[4]);
    }
    if (args.length>5) {
      timeoutInMinutes = Long.valueOf(args[5]);
    }

    MetricsDataMigrationLauncher dataMigrationLauncher = null;
    try {
      LOG.info("Initializing system...");
      dataMigrationLauncher = new MetricsDataMigrationLauncher(whitelistedFilePath, processedMetricsFilePath, startTime, numberOfThreads, batchSize);
    } catch (Exception e) {
      LOG.error("Exception during system setup, exiting...", e);
      System.exit(1);
    }

    try {
      //Setup shutdown hook for metadata save.
      MetricsDataMigrationLauncher finalDataMigrationLauncher = dataMigrationLauncher;
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            finalDataMigrationLauncher.saveMetadata();
          } catch (SQLException e) {
            LOG.error("Exception during metadata saving, exiting...", e);
          }
        }
      });

      dataMigrationLauncher.runMigration(timeoutInMinutes);
    } catch (IOException e) {
      LOG.error("Exception during data migration, exiting...", e);
      System.exit(1);
    }

    System.exit(0);

  }
}
