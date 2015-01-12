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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.PhoenixTransactSQL.Condition;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
    .timeline.PhoenixTransactSQL.LikeCondition;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.HBASE_SITE_CONFIGURATION_FILE;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.METRICS_SITE_CONFIGURATION_FILE;

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

  @Override
  public TimelineMetrics getTimelineMetrics(List<String> metricNames,
      String hostname, String applicationId, String instanceId,
      Long startTime, Long endTime, Integer limit,
      boolean groupedByHosts) throws SQLException, IOException {

    Condition condition = new LikeCondition(metricNames, hostname, applicationId,
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
      new LikeCondition(Collections.singletonList(metricName), hostname,
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
