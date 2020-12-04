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
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
  private static final String PATTERN_PREFIX = "._p_";
  private static final int DEFAULT_BATCH_SIZE = 5;
  private static final String MIGRATE_ALL_METRICS_ARG = "--allmetrics";
  public static final Map<String, String> CLUSTER_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final Map<String, String> HOST_AGGREGATE_TABLES_MAPPING = new HashMap<>();
  public static final String DEFAULT_PROCESSED_METRICS_FILE_LOCATION = "/var/log/ambari-metrics-collector/ambari-metrics-migration-state.txt";
  public static final int DEFAULT_NUMBER_OF_THREADS = 3;
  public static final int DEFAULT_START_DAYS = 30; // 30 Days, Last month

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

  private final long startTimeEpoch;
  private final int numberOfThreads;
  private TimelineMetricConfiguration timelineMetricConfiguration;
  private PhoenixHBaseAccessor hBaseAccessor;
  private TimelineMetricMetadataManager timelineMetricMetadataManager;
  private Map<String, Set<String>> processedMetrics;

  public MetricsDataMigrationLauncher(String whitelistedFilePath, String processedMetricsFilePath, Long startDay, Integer numberOfThreads, Integer batchSize) throws Exception {
    this.startTimeEpoch = calculateStartEpochTime(startDay);
    this.numberOfThreads = (numberOfThreads == null) ? DEFAULT_NUMBER_OF_THREADS : numberOfThreads;
    this.processedMetricsFilePath = (processedMetricsFilePath == null) ? DEFAULT_PROCESSED_METRICS_FILE_LOCATION : processedMetricsFilePath;

    initializeHbaseAccessor();
    readProcessedMetricsMap();

    final Set<String> metricNames = getMetricNames(whitelistedFilePath);

    LOG.info("Setting up batches...");
    if (batchSize == null) batchSize = DEFAULT_BATCH_SIZE;
    this.metricNamesBatches = new HashSet<>(batchSize);

    Iterables.partition(metricNames, batchSize)
      .forEach(batch -> metricNamesBatches.add(new HashSet<>(batch)));
    LOG.info(String.format("Split metric names into %s batches with size of %s", metricNamesBatches.size(), batchSize));
  }

  private long calculateStartEpochTime(Long startDay) {
    final long days;
    if (startDay == null) {
      LOG.info(String.format("No starting day have been provided, using default: %d", DEFAULT_START_DAYS));
      days = DEFAULT_START_DAYS;
    } else {
      LOG.info(String.format("%d days have been provided as migration starting day.", startDay));
      days = startDay;
    }
    LOG.info(String.format("The last %d days' data will be migrated.", days));

    return LocalDateTime.now().minusDays(days).toEpochSecond(ZoneOffset.UTC);
  }

  private Set<String> getMetricNames(String whitelistedFilePath) throws MalformedURLException, URISyntaxException, SQLException {
    if(StringUtils.isNotEmpty(whitelistedFilePath) && whitelistedFilePath.equalsIgnoreCase(MIGRATE_ALL_METRICS_ARG)) {
      LOG.info("Migration of all metrics has been requested by the " + MIGRATE_ALL_METRICS_ARG + " argument.");
      LOG.info("Looking for all the metric names in the Metrics Database...");
      return this.hBaseAccessor.getTimelineMetricMetadataV1().keySet().stream()
          .map(TimelineMetricMetadataKey::getMetricName).collect(Collectors.toSet());
    }

    if(StringUtils.isNotEmpty(whitelistedFilePath)) {
      LOG.info(String.format("Whitelist file %s has been provided.", whitelistedFilePath));
      LOG.info("Looking for whitelisted metric names based on the file content...");
      return readMetricWhitelistFromFile(whitelistedFilePath);
    }

    final Configuration conf = this.timelineMetricConfiguration.getMetricsConf();
    if (Boolean.parseBoolean(conf.get(TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_ENABLED))) {
      whitelistedFilePath = conf.get(TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE,
              TimelineMetricConfiguration.TIMELINE_METRICS_WHITELIST_FILE_LOCATION_DEFAULT);
      LOG.info(String.format("No whitelist file has been provided but Ambari Metrics Whitelisting is enabled. " +
              "Using %s as whitelist file.", whitelistedFilePath));
      LOG.info("Looking for whitelisted metric names based on the file content...");
      return readMetricWhitelistFromFile(whitelistedFilePath);
    }

    LOG.info("No whitelist file has been provided and Ambari Metrics Whitelisting is disabled.");
    LOG.info("Looking for all the metric names in the Metrics Database...");
    return this.hBaseAccessor.getTimelineMetricMetadataV1().keySet().stream()
        .map(TimelineMetricMetadataKey::getMetricName).collect(Collectors.toSet());
  }

  private void readProcessedMetricsMap() {
    final Map<String, Set<String>> result = new HashMap<>();
    final Path path = Paths.get(this.processedMetricsFilePath);

    if (Files.notExists(path)) {
      LOG.info(String.format("The processed metrics file %s is missing, assuming there were no metrics processed.", this.processedMetricsFilePath));
    } else {
      LOG.info(String.format("Reading the list of already copied metrics from %s", this.processedMetricsFilePath));
      try {
        try (Stream<String> stream = Files.lines(path)) {
          stream.forEach(line -> {
            String[] lineSplit = line.split(":");
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
    }
    this.processedMetrics = result;
  }

  public void runMigration(Long timeoutInMinutes) throws IOException {
    try (Writer processedMetricsFileWriter = new BufferedWriter(new FileWriter(this.processedMetricsFilePath, true))) {
      LOG.info("Setting up copiers...");
      Set<AbstractPhoenixMetricsCopier> copiers = new HashSet<>();
      for (Set<String> batch : metricNamesBatches) {
        for (Map.Entry<String, String> entry : CLUSTER_AGGREGATE_TABLES_MAPPING.entrySet()) {
          Set<String> filteredMetrics = filterProcessedMetrics(batch, this.processedMetrics, entry.getKey());
          if (!filteredMetrics.isEmpty()) {
            copiers.add(new PhoenixClusterMetricsCopier(entry.getKey(), entry.getValue(), this.hBaseAccessor,
                filteredMetrics, this.startTimeEpoch, processedMetricsFileWriter));
          }
        }

        for (Map.Entry<String, String> entry : HOST_AGGREGATE_TABLES_MAPPING.entrySet()) {
          Set<String> filteredMetrics = filterProcessedMetrics(batch, this.processedMetrics, entry.getKey());
          if (!filteredMetrics.isEmpty()) {
            copiers.add(new PhoenixHostMetricsCopier(entry.getKey(), entry.getValue(), this.hBaseAccessor,
                filteredMetrics, this.startTimeEpoch, processedMetricsFileWriter));
          }
        }
      }

      if (copiers.isEmpty()) {
        LOG.info("No copy threads to run, looks like all metrics have been copied.");
        return;
      }

      LOG.info("Running the copy threads...");
      long timerStart = System.currentTimeMillis();
      ExecutorService executorService = null;
      try {
        executorService = Executors.newFixedThreadPool(this.numberOfThreads);
        for (AbstractPhoenixMetricsCopier copier : copiers) {
          executorService.submit(copier);
        }
      } finally {
        if (executorService != null) {
          executorService.shutdown();
          try {
            executorService.awaitTermination(timeoutInMinutes, TimeUnit.MINUTES);
          } catch (InterruptedException e) {
            LOG.error(e);
          }
        }
      }

      long timerDelta = System.currentTimeMillis() - timerStart;
      LOG.info(String.format("Copying took %s seconds", timerDelta / 1000.0));
    }
  }

  private void initializeHbaseAccessor() throws MalformedURLException, URISyntaxException {
    this.hBaseAccessor = new PhoenixHBaseAccessor(null);
    this.timelineMetricConfiguration = TimelineMetricConfiguration.getInstance();
    this.timelineMetricConfiguration.initialize();

    this.timelineMetricMetadataManager = new TimelineMetricMetadataManager(this.hBaseAccessor);
    this.timelineMetricMetadataManager.initializeMetadata(false);

    this.hBaseAccessor.setMetadataInstance(this.timelineMetricMetadataManager);
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
    final Set<String> whitelistedMetrics = new HashSet<>();

    String strLine;

    try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(whitelistFile)))) {
      while ((strLine = br.readLine()) != null)   {
        strLine = strLine.trim();
        if (StringUtils.isEmpty(strLine)) {
          continue;
        }
        if (strLine.startsWith(PATTERN_PREFIX)) {
          strLine = strLine.replace(PATTERN_PREFIX, "");
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
    this.timelineMetricMetadataManager.updateMetadataCacheUsingV1Tables();
    this.timelineMetricMetadataManager.forceMetricsMetadataSync();
    LOG.info("Metadata was saved.");
  }

  /**
   *
   * @param args
   * REQUIRED args[0] - processedMetricsFilePath - full path to the file where processed metric are/will be stored
   *
   * OPTIONAL args[1] - whitelistedFilePath      - full path to the file with whitelisted metrics filenames
   *                                               if not provided and AMS whitelisting is enabled the default whitelist
   *                                               file location will be used if configured
   *                                               if not provided and AMS whitelisting is disabled then no whitelisting
   *                                               will be used and all the metrics will be migrated
   *                                               if --allmetrics switch is provided then all the metrics will be migrated
   *                                               regardless to other settings
   *          args[2] - startDay                 - default value is set to the last 30 days
   *          args[3] - numberOfThreads          - default value is 3
   *          args[4] - batchSize                - default value is 5
   *          args[5] - timeoutInMinutes         - default value is set to the equivalent of 24 hours
   */
  public static void main(String[] args) {
    String processedMetricsFilePath = null;
    String whitelistedFilePath = null;
    Long startDay = null;
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
      startDay = Long.valueOf(args[2]);
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
      dataMigrationLauncher = new MetricsDataMigrationLauncher(whitelistedFilePath, processedMetricsFilePath, startDay, numberOfThreads, batchSize);
    } catch (Exception e) {
      LOG.error("Exception during system setup, exiting...", e);
      System.exit(1);
    }

    int exitCode = 0;
    try {
      dataMigrationLauncher.runMigration(timeoutInMinutes);
    } catch (Throwable e) {
      exitCode = 1;
      LOG.error("Exception during data migration, exiting...", e);
    } finally {
      try {
        dataMigrationLauncher.saveMetadata();
      } catch (SQLException e) {
        exitCode = 1;
        LOG.error("Exception while saving the Metadata, exiting...", e);
      }
    }

    if(exitCode == 0) LOG.info("Data migration finished successfully.");

    System.exit(exitCode);
  }
}
