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
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.AGGREGATOR_CHECKPOINT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.USE_GROUPBY_AGGREGATOR_QUERIES;

public class HBaseTimelineMetricStore extends AbstractService implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricStore.class);
  private final TimelineMetricConfiguration configuration;
  private PhoenixHBaseAccessor hBaseAccessor;
  private static volatile boolean isInitialized = false;
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  private TimelineMetricMetadataManager metricMetadataManager;

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

      if (Boolean.parseBoolean(metricsConf.get(USE_GROUPBY_AGGREGATOR_QUERIES, "true"))) {
        LOG.info("Using group by aggregators for aggregating host and cluster metrics.");
      }

      // Start the cluster aggregator second
      TimelineMetricAggregator secondClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hBaseAccessor, metricsConf, metricMetadataManager);
      scheduleAggregatorThread(secondClusterAggregator);

      // Start the minute cluster aggregator
      TimelineMetricAggregator minuteClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(minuteClusterAggregator);

      // Start the hourly cluster aggregator
      TimelineMetricAggregator hourlyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(hourlyClusterAggregator);

      // Start the daily cluster aggregator
      TimelineMetricAggregator dailyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(dailyClusterAggregator);

      // Start the minute host aggregator
      TimelineMetricAggregator minuteHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(minuteHostAggregator);

      // Start the hourly host aggregator
      TimelineMetricAggregator hourlyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(hourlyHostAggregator);

      // Start the daily host aggregator
      TimelineMetricAggregator dailyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorDaily(hBaseAccessor, metricsConf);
      scheduleAggregatorThread(dailyHostAggregator);

      if (!configuration.isTimelineMetricsServiceWatcherDisabled()) {
        int initDelay = configuration.getTimelineMetricsServiceWatcherInitDelay();
        int delay = configuration.getTimelineMetricsServiceWatcherDelay();
        // Start the watchdog
        executorService.scheduleWithFixedDelay(
          new TimelineMetricStoreWatcher(this, configuration), initDelay, delay,
          TimeUnit.SECONDS);
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
      boolean groupedByHosts) throws SQLException, IOException {

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

    Condition condition = new DefaultCondition(
      new ArrayList<String>(metricFunctions.keySet()),
      hostnames, applicationId, instanceId, startTime, endTime,
      precision, limit, groupedByHosts);

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
        updateValuesAsRate(metric.getMetricValues());
      }
    }

    return metrics;
  }

  static Map<Long, Double> updateValuesAsRate(Map<Long, Double> metricValues) {
    Long prevTime = null;
    Double prevVal = null;
    long step;
    Double diff;

    for (Map.Entry<Long, Double> timeValueEntry : metricValues.entrySet()) {
      Long currTime = timeValueEntry.getKey();
      Double currVal = timeValueEntry.getValue();

      if (prevTime != null) {
        step = currTime - prevTime;
        diff = currVal - prevVal;
        Double rate = diff / TimeUnit.MILLISECONDS.toSeconds(step);
        timeValueEntry.setValue(rate);
      } else {
        timeValueEntry.setValue(0.0);
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
  public TimelineMetric getTimelineMetric(String metricName, List<String> hostnames,
      String applicationId, String instanceId, Long startTime,
      Long endTime, Precision precision, Integer limit)
      throws SQLException, IOException {

    if (metricName == null || metricName.isEmpty()) {
      throw new IllegalArgumentException("No metric name filter specified.");
    }
    if ((startTime == null && endTime != null)
        || (startTime != null && endTime == null)) {
      throw new IllegalArgumentException("Open ended query not supported ");
    }
    if (limit !=null && limit > PhoenixHBaseAccessor.RESULTSET_LIMIT){
      throw new IllegalArgumentException("Limit too big");
    }

    Map<String, List<Function>> metricFunctions =
      parseMetricNamesToAggregationFunctions(Collections.singletonList(metricName));

    Condition condition = new DefaultCondition(
      new ArrayList<String>(metricFunctions.keySet()), hostnames, applicationId,
      instanceId, startTime, endTime, precision, limit, true);
    TimelineMetrics metrics = hBaseAccessor.getMetricRecords(condition,
      metricFunctions);

    metrics = postProcessMetrics(metrics);

    TimelineMetric metric = new TimelineMetric();
    List<TimelineMetric> metricList = metrics.getMetrics();

    if (metricList != null && !metricList.isEmpty()) {
      metric.setMetricName(metricList.get(0).getMetricName());
      metric.setAppId(metricList.get(0).getAppId());
      metric.setInstanceId(metricList.get(0).getInstanceId());
      metric.setHostName(metricList.get(0).getHostName());
      // Assumption that metrics are ordered by start time
      metric.setStartTime(metricList.get(0).getStartTime());
      TreeMap<Long, Double> metricRecords = new TreeMap<Long, Double>();
      for (TimelineMetric timelineMetric : metricList) {
        metricRecords.putAll(timelineMetric.getMetricValues());
      }
      metric.setMetricValues(metricRecords);
    }

    return metric;
  }

  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics) throws SQLException, IOException {
    // Error indicated by the Sql exception
    TimelinePutResponse response = new TimelinePutResponse();

    hBaseAccessor.insertMetricRecordsWithMetadata(metricMetadataManager, metrics);

    return response;
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

  private void scheduleAggregatorThread(TimelineMetricAggregator aggregator) {
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    if (!aggregator.isDisabled()) {
      executorService.scheduleAtFixedRate(aggregator,
        0l,
        aggregator.getSleepIntervalMillis(),
        TimeUnit.MILLISECONDS);
    }
  }
}
