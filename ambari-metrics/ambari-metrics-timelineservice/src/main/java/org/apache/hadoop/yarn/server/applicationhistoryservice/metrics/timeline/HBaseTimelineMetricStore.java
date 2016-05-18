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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.ContainerMetric;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.TimelineMetricHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.ConditionBuilder;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.TopNCondition;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.USE_GROUPBY_AGGREGATOR_QUERIES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DEFAULT_TOPN_HOSTS_LIMIT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.ACTUAL_AGGREGATOR_NAMES;

public class HBaseTimelineMetricStore extends AbstractService implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricStore.class);
  private final TimelineMetricConfiguration configuration;
  private PhoenixHBaseAccessor hBaseAccessor;
  private static volatile boolean isInitialized = false;
  private final ScheduledExecutorService watchdogExecutorService = Executors.newSingleThreadScheduledExecutor();
  private final Map<AGGREGATOR_NAME, ScheduledExecutorService> scheduledExecutors = new HashMap<>();
  private TimelineMetricMetadataManager metricMetadataManager;
  private TimelineMetricHAController haController;
  private Integer defaultTopNHostsLimit;

  /**
   * Construct the service.
   *
   */
  public HBaseTimelineMetricStore(TimelineMetricConfiguration configuration) {
    super(HBaseTimelineMetricStore.class.getName());
    this.configuration = configuration;
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {
    super.serviceInit(conf);
    initializeSubsystem(configuration.getHbaseConf(), configuration.getMetricsConf());
  }

  private synchronized void initializeSubsystem(Configuration hbaseConf,
                                                Configuration metricsConf) {
    if (!isInitialized) {
      hBaseAccessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf);
      // Initialize schema
      hBaseAccessor.initMetricSchema();
      // Initialize metadata from store
      metricMetadataManager = new TimelineMetricMetadataManager(hBaseAccessor, metricsConf);
      metricMetadataManager.initializeMetadata();
      // Initialize policies before TTL update
      hBaseAccessor.initPoliciesAndTTL();
      // Start HA service
      if (configuration.isDistributedOperationModeEnabled()) {
        // Start the controller
        haController = new TimelineMetricHAController(configuration);
        try {
          haController.initializeHAController();
        } catch (Exception e) {
          LOG.error(e);
          throw new MetricsSystemInitializationException("Unable to " +
            "initialize HA controller", e);
        }
      }

      defaultTopNHostsLimit = Integer.parseInt(metricsConf.get(DEFAULT_TOPN_HOSTS_LIMIT, "20"));
      if (Boolean.parseBoolean(metricsConf.get(USE_GROUPBY_AGGREGATOR_QUERIES, "true"))) {
        LOG.info("Using group by aggregators for aggregating host and cluster metrics.");
      }

      // Start the cluster aggregator second
      TimelineMetricAggregator secondClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(secondClusterAggregator);

      // Start the minute cluster aggregator
      TimelineMetricAggregator minuteClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(minuteClusterAggregator);

      // Start the hourly cluster aggregator
      TimelineMetricAggregator hourlyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(hourlyClusterAggregator);

      // Start the daily cluster aggregator
      TimelineMetricAggregator dailyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(dailyClusterAggregator);

      // Start the minute host aggregator
      TimelineMetricAggregator minuteHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(minuteHostAggregator);

      // Start the hourly host aggregator
      TimelineMetricAggregator hourlyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(hourlyHostAggregator);

      // Start the daily host aggregator
      TimelineMetricAggregator dailyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorDaily(
          hBaseAccessor, metricsConf, haController);
      scheduleAggregatorThread(dailyHostAggregator);

      if (!configuration.isTimelineMetricsServiceWatcherDisabled()) {
        int initDelay = configuration.getTimelineMetricsServiceWatcherInitDelay();
        int delay = configuration.getTimelineMetricsServiceWatcherDelay();
        // Start the watchdog
        watchdogExecutorService.scheduleWithFixedDelay(
          new TimelineMetricStoreWatcher(this, configuration),
          initDelay, delay, TimeUnit.SECONDS);
        LOG.info("Started watchdog for timeline metrics store with initial " +
          "delay = " + initDelay + ", delay = " + delay);
      }

      isInitialized = true;
    }

  }

  @Override
  protected void serviceStop() throws Exception {
    super.serviceStop();
  }

  @Override
  public TimelineMetrics getTimelineMetrics(List<String> metricNames,
      List<String> hostnames, String applicationId, String instanceId,
      Long startTime, Long endTime, Precision precision, Integer limit,
      boolean groupedByHosts, TopNConfig topNConfig) throws SQLException, IOException {

    if (metricNames == null || metricNames.isEmpty()) {
      throw new IllegalArgumentException("No metric name filter specified.");
    }
    if ((startTime == null && endTime != null)
        || (startTime != null && endTime == null)) {
      throw new IllegalArgumentException("Open ended query not supported ");
    }
    if (limit != null && limit > PhoenixHBaseAccessor.RESULTSET_LIMIT){
      throw new IllegalArgumentException("Limit too big");
    }
    Map<String, List<Function>> metricFunctions =
      parseMetricNamesToAggregationFunctions(metricNames);

    ConditionBuilder conditionBuilder = new ConditionBuilder(new ArrayList<String>(metricFunctions.keySet()))
      .hostnames(hostnames)
      .appId(applicationId)
      .instanceId(instanceId)
      .startTime(startTime)
      .endTime(endTime)
      .precision(precision)
      .limit(limit)
      .grouped(groupedByHosts);

    if (topNConfig != null) {
      if (TopNCondition.isTopNHostCondition(metricNames, hostnames) || TopNCondition.isTopNMetricCondition(metricNames, hostnames)) {
        conditionBuilder.topN(topNConfig.getTopN());
        conditionBuilder.isBottomN(topNConfig.getIsBottomN());
        Function.ReadFunction readFunction = Function.ReadFunction.getFunction(topNConfig.getTopNFunction());
        Function function = new Function(readFunction, null);
        conditionBuilder.topNFunction(function);
      } else {
        LOG.info("Invalid Input for TopN query. Ignoring TopN Request.");
      }
    } else if (hostnames != null && hostnames.size() > defaultTopNHostsLimit) {
      LOG.info("Requesting data for more than " + defaultTopNHostsLimit +  " Hosts. " +
        "Defaulting to Top " + defaultTopNHostsLimit);
      conditionBuilder.topN(defaultTopNHostsLimit);
      conditionBuilder.isBottomN(false);
    }

    Condition condition = conditionBuilder.build();

    TimelineMetrics metrics;

    if (hostnames == null || hostnames.isEmpty()) {
      metrics = hBaseAccessor.getAggregateMetricRecords(condition, metricFunctions);
    } else {
      metrics = hBaseAccessor.getMetricRecords(condition, metricFunctions);
    }
    return postProcessMetrics(metrics);
  }

  private TimelineMetrics postProcessMetrics(TimelineMetrics metrics) {
    List<TimelineMetric> metricsList = metrics.getMetrics();

    for (TimelineMetric metric : metricsList){
      String name = metric.getMetricName();
      if (name.contains("._rate")){
        updateValuesAsRate(metric.getMetricValues(), false);
      } else if (name.contains("._diff")) {
        updateValuesAsRate(metric.getMetricValues(), true);
      }
    }

    return metrics;
  }

  static Map<Long, Double> updateValuesAsRate(Map<Long, Double> metricValues, boolean isDiff) {
    Long prevTime = null;
    Double prevVal = null;
    long step;
    Double diff;

    for(Iterator<Map.Entry<Long, Double>> it = metricValues.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Long, Double> timeValueEntry = it.next();
      Long currTime = timeValueEntry.getKey();
      Double currVal = timeValueEntry.getValue();

      if (prevTime != null) {
        step = currTime - prevTime;
        diff = currVal - prevVal;
        Double rate = isDiff ? diff : (diff / TimeUnit.MILLISECONDS.toSeconds(step));
        timeValueEntry.setValue(rate);
      } else {
        it.remove();
      }

      prevTime = currTime;
      prevVal = currVal;
    }

    return metricValues;
  }

  static HashMap<String, List<Function>> parseMetricNamesToAggregationFunctions(List<String> metricNames) {
    HashMap<String, List<Function>> metricsFunctions = new HashMap<>();

    for (String metricName : metricNames){
      Function function = Function.DEFAULT_VALUE_FUNCTION;
      String cleanMetricName = metricName;

      try {
        function = Function.fromMetricName(metricName);
        int functionStartIndex = metricName.indexOf("._");
        if (functionStartIndex > 0) {
          cleanMetricName = metricName.substring(0, functionStartIndex);
        }
      } catch (Function.FunctionFormatException ffe){
        // unknown function so
        // fallback to VALUE, and fullMetricName
      }

      List<Function> functionsList = metricsFunctions.get(cleanMetricName);
      if (functionsList == null) {
        functionsList = new ArrayList<>(1);
      }
      functionsList.add(function);
      metricsFunctions.put(cleanMetricName, functionsList);
    }

    return metricsFunctions;
  }

  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics) throws SQLException, IOException {
    // Error indicated by the Sql exception
    TimelinePutResponse response = new TimelinePutResponse();

    hBaseAccessor.insertMetricRecordsWithMetadata(metricMetadataManager, metrics);

    return response;
  }

  @Override
  public TimelinePutResponse putContainerMetrics(List<ContainerMetric> metrics)
      throws SQLException, IOException {
    hBaseAccessor.insertContainerMetrics(metrics);
    return new TimelinePutResponse();
  }

  @Override
  public Map<String, List<TimelineMetricMetadata>> getTimelineMetricMetadata() throws SQLException, IOException {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata =
      metricMetadataManager.getMetadataCache();

    // Group Metadata by AppId
    Map<String, List<TimelineMetricMetadata>> metadataByAppId = new HashMap<>();
    for (TimelineMetricMetadata metricMetadata : metadata.values()) {
      List<TimelineMetricMetadata> metadataList = metadataByAppId.get(metricMetadata.getAppId());
      if (metadataList == null) {
        metadataList = new ArrayList<>();
        metadataByAppId.put(metricMetadata.getAppId(), metadataList);
      }

      metadataList.add(metricMetadata);
    }

    return metadataByAppId;
  }

  @Override
  public Map<String, Set<String>> getHostAppsMetadata() throws SQLException, IOException {
    return metricMetadataManager.getHostedAppsCache();
  }

  @Override
  public List<String> getLiveInstances() {
    return haController.getLiveInstanceHostNames();
  }

  private void scheduleAggregatorThread(final TimelineMetricAggregator aggregator) {
    if (!aggregator.isDisabled()) {
      ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r, ACTUAL_AGGREGATOR_NAMES.get(aggregator.getName()));
          }
        }
      );
      scheduledExecutors.put(aggregator.getName(), executorService);
      executorService.scheduleAtFixedRate(aggregator,
        0l,
        aggregator.getSleepIntervalMillis(),
        TimeUnit.MILLISECONDS);
      LOG.info("Scheduled aggregator thread " + aggregator.getName() + " every " +
        + aggregator.getSleepIntervalMillis() + " milliseconds.");
    } else {
      LOG.info("Skipped scheduling " + aggregator.getName() + " since it is disabled.");
    }
  }
}
