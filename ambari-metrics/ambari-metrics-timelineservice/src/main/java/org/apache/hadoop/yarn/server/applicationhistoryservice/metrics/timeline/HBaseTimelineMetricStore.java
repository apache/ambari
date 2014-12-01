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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.PhoenixTransactSQL.Condition;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.HBASE_SITE_CONFIGURATION_FILE;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.METRICS_SITE_CONFIGURATION_FILE;

public class HBaseTimelineMetricStore extends AbstractService
    implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricStore.class);
  private PhoenixHBaseAccessor hBaseAccessor;

  /**
   * Construct the service.
   *
   */
  public HBaseTimelineMetricStore() {
    super(HBaseTimelineMetricStore.class.getName());
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {
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

    Configuration hbaseConf = new Configuration(true);
    hbaseConf.addResource(hbaseResUrl.toURI().toURL());
    Configuration metricsConf = new Configuration(true);
    metricsConf.addResource(amsResUrl.toURI().toURL());

    initializeSubsystem(hbaseConf, metricsConf);
  }

  private void initializeSubsystem(Configuration hbaseConf,
                                   Configuration metricsConf) {
    hBaseAccessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf);
    hBaseAccessor.initMetricSchema();

    // Start the cluster aggregator
    TimelineMetricClusterAggregator minuteClusterAggregator =
      new TimelineMetricClusterAggregator(hBaseAccessor, metricsConf);
    if (!minuteClusterAggregator.isDisabled()) {
      Thread aggregatorThread = new Thread(minuteClusterAggregator);
      aggregatorThread.start();
    }

    // Start the cluster aggregator hourly
    TimelineMetricClusterAggregatorHourly hourlyClusterAggregator =
      new TimelineMetricClusterAggregatorHourly(hBaseAccessor, metricsConf);
    if (!hourlyClusterAggregator.isDisabled()) {
      Thread aggregatorThread = new Thread(hourlyClusterAggregator);
      aggregatorThread.start();
    }

    // Start the 5 minute aggregator
    TimelineMetricAggregator minuteHostAggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute
        (hBaseAccessor, metricsConf);
    if (!minuteHostAggregator.isDisabled()) {
      Thread minuteAggregatorThread = new Thread(minuteHostAggregator);
      minuteAggregatorThread.start();
    }

    // Start hourly host aggregator
    TimelineMetricAggregator hourlyHostAggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly
        (hBaseAccessor, metricsConf);
    if (!hourlyHostAggregator.isDisabled()) {
      Thread aggregatorHourlyThread = new Thread(hourlyHostAggregator);
      aggregatorHourlyThread.start();
    }
  }

  @Override
  protected void serviceStop() throws Exception {
    super.serviceStop();
  }

  //TODO: update to work with HOSTS_COUNT and METRIC_COUNT
  @Override
  public TimelineMetrics getTimelineMetrics(List<String> metricNames,
      String hostname, String applicationId, String instanceId,
      Long startTime, Long endTime, Integer limit,
      boolean groupedByHosts) throws SQLException, IOException {

    Condition condition = new Condition(metricNames, hostname, applicationId,
      instanceId, startTime, endTime, limit, groupedByHosts);

    if (hostname == null) {
      return hBaseAccessor.getAggregateMetricRecords(condition);
    }

    return hBaseAccessor.getMetricRecords(condition);
  }

  @Override
  public TimelineMetric getTimelineMetric(String metricName, String hostname,
      String applicationId, String instanceId, Long startTime,
      Long endTime, Integer limit)
      throws SQLException, IOException {

    TimelineMetrics metrics = hBaseAccessor.getMetricRecords(
      new Condition(Collections.singletonList(metricName), hostname,
        applicationId, instanceId, startTime, endTime, limit, true)
    );

    TimelineMetric metric = new TimelineMetric();
    List<TimelineMetric> metricList = metrics.getMetrics();

    if (metricList != null && !metricList.isEmpty()) {
      metric.setMetricName(metricList.get(0).getMetricName());
      metric.setAppId(metricList.get(0).getAppId());
      metric.setInstanceId(metricList.get(0).getInstanceId());
      metric.setHostName(metricList.get(0).getHostName());
      // Assumption that metrics are ordered by start time
      metric.setStartTime(metricList.get(0).getStartTime());
      Map<Long, Double> metricRecords = new HashMap<Long, Double>();
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
