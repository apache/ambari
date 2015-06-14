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
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HBaseTimelineMetricStore extends AbstractService
    implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricStore.class);
  private final TimelineMetricConfiguration configuration;
  private PhoenixHBaseAccessor hBaseAccessor;

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

  private void initializeSubsystem(Configuration hbaseConf,
                                   Configuration metricsConf) {
    hBaseAccessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf);
    hBaseAccessor.initMetricSchema();

    // Start the cluster aggregator minute
    TimelineMetricAggregator minuteClusterAggregator =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(hBaseAccessor, metricsConf);
    if (!minuteClusterAggregator.isDisabled()) {
      Thread aggregatorThread = new Thread(minuteClusterAggregator);
      aggregatorThread.start();
    }

    // Start the hourly cluster aggregator
    TimelineMetricAggregator hourlyClusterAggregator =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hBaseAccessor, metricsConf);
    if (!hourlyClusterAggregator.isDisabled()) {
      Thread aggregatorThread = new Thread(hourlyClusterAggregator);
      aggregatorThread.start();
    }

    // Start the daily cluster aggregator
    TimelineMetricAggregator dailyClusterAggregator =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(hBaseAccessor, metricsConf);
    if (!dailyClusterAggregator.isDisabled()) {
      Thread aggregatorThread = new Thread(dailyClusterAggregator);
      aggregatorThread.start();
    }

    // Start the minute host aggregator
    TimelineMetricAggregator minuteHostAggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hBaseAccessor, metricsConf);
    if (!minuteHostAggregator.isDisabled()) {
      Thread minuteAggregatorThread = new Thread(minuteHostAggregator);
      minuteAggregatorThread.start();
    }

    // Start the hourly host aggregator
    TimelineMetricAggregator hourlyHostAggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(hBaseAccessor, metricsConf);
    if (!hourlyHostAggregator.isDisabled()) {
      Thread aggregatorHourlyThread = new Thread(hourlyHostAggregator);
      aggregatorHourlyThread.start();
    }

    // Start the daily host aggregator
    TimelineMetricAggregator dailyHostAggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorDaily(hBaseAccessor, metricsConf);
    if (!dailyHostAggregator.isDisabled()) {
      Thread aggregatorDailyThread = new Thread(dailyHostAggregator);
      aggregatorDailyThread.start();
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
      metrics = hBaseAccessor.getAggregateMetricRecords(condition,
          metricFunctions);
    } else {
      metrics = hBaseAccessor.getMetricRecords(condition, metricFunctions);
    }
    return postProcessMetrics(metrics);
  }

  private TimelineMetrics postProcessMetrics(TimelineMetrics metrics) {
    List<TimelineMetric> metricsList = metrics.getMetrics();

    for (TimelineMetric metric: metricsList){
      String name = metric.getMetricName();
      if (name.contains("._rate")){
        updateValueAsRate(metric.getMetricValues());
      }
    }

    return metrics;
  }

  private Map<Long, Double> updateValueAsRate(Map<Long, Double> metricValues) {
    Long prevTime = null;
    long step;

    for (Map.Entry<Long, Double> timeValueEntry : metricValues.entrySet()) {
      Long currTime = timeValueEntry.getKey();
      Double currVal = timeValueEntry.getValue();

      if (prevTime != null) {
        step = currTime - prevTime;
        Double rate = currVal / step;
        timeValueEntry.setValue(rate);
      } else {
        timeValueEntry.setValue(0.0);
      }

      prevTime = currTime;
    }

    return metricValues;
  }

  public static HashMap<String, List<Function>> parseMetricNamesToAggregationFunctions(List<String> metricNames) {
    HashMap<String, List<Function>> metricsFunctions = new HashMap<String,
      List<Function>>();

    for (String metricName : metricNames){
      Function function = Function.DEFAULT_VALUE_FUNCTION;
      String cleanMetricName = metricName;

      try {
        function = Function.fromMetricName(metricName);
        int functionStartIndex = metricName.indexOf("._");
        if(functionStartIndex > 0 ) {
          cleanMetricName = metricName.substring(0, functionStartIndex);
        }
      } catch (Function.FunctionFormatException ffe){
        // unknown function so
        // fallback to VALUE, and fullMetricName
      }

      addFunctionToMetricName(metricsFunctions, cleanMetricName, function);
    }

    return metricsFunctions;
  }

  private static void addFunctionToMetricName(
    HashMap<String, List<Function>> metricsFunctions, String cleanMetricName,
    Function function) {

    List<Function> functionsList = metricsFunctions.get(cleanMetricName);
    if (functionsList==null) functionsList = new ArrayList<Function>(1);
    functionsList.add(function);
    metricsFunctions.put(cleanMetricName, functionsList);
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
      Map<Long, Double> metricRecords = new TreeMap<Long, Double>();
      for (TimelineMetric timelineMetric : metricList) {
        metricRecords.putAll(timelineMetric.getMetricValues());
      }
      metric.setMetricValues(metricRecords);
    }

    return metric;
  }


  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics)
    throws SQLException, IOException {

    // Error indicated by the Sql exception
    TimelinePutResponse response = new TimelinePutResponse();

    hBaseAccessor.insertMetricRecords(metrics);

    return response;
  }
}
