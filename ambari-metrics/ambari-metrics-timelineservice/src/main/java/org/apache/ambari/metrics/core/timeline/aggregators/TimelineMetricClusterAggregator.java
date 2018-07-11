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
package org.apache.ambari.metrics.core.timeline.aggregators;

import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.metrics.core.timeline.availability.MetricCollectorHAController;
import org.apache.ambari.metrics.core.timeline.query.DefaultCondition;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.ambari.metrics.core.timeline.query.Condition;

public class TimelineMetricClusterAggregator extends AbstractTimelineAggregator {
  private final TimelineMetricReadHelper readHelper;
  private final boolean isClusterPrecisionInputTable;

  public TimelineMetricClusterAggregator(AGGREGATOR_NAME aggregatorName,
                                         TimelineMetricMetadataManager metricMetadataManager,
                                         PhoenixHBaseAccessor hBaseAccessor,
                                         Configuration metricsConf,
                                         String checkpointLocation,
                                         Long sleepIntervalMillis,
                                         Integer checkpointCutOffMultiplier,
                                         String hostAggregatorDisabledParam,
                                         String inputTableName,
                                         String outputTableName,
                                         Long nativeTimeRangeDelay,
                                         MetricCollectorHAController haController) {
    super(aggregatorName, hBaseAccessor, metricsConf, checkpointLocation,
      sleepIntervalMillis, checkpointCutOffMultiplier,
      hostAggregatorDisabledParam, inputTableName, outputTableName,
      nativeTimeRangeDelay, haController);
    isClusterPrecisionInputTable = inputTableName.equals(METRICS_CLUSTER_AGGREGATE_TABLE_NAME);
    readHelper = new TimelineMetricReadHelper(metricMetadataManager, true);
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    String sqlStr = String.format(GET_CLUSTER_AGGREGATE_TIME_SQL, tableName);
    // HOST_COUNT vs METRIC_COUNT
    if (isClusterPrecisionInputTable) {
      sqlStr = String.format(GET_CLUSTER_AGGREGATE_SQL, tableName);
    }

    condition.setStatement(sqlStr);
    condition.addOrderByColumn("UUID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws IOException, SQLException {
    Map<TimelineClusterMetric, MetricHostAggregate> hostAggregateMap = aggregateMetricsFromResultSet(rs, endTime);

    LOG.info("Saving " + hostAggregateMap.size() + " metric aggregates.");
    hBaseAccessor.saveClusterAggregateRecordsSecond(hostAggregateMap, outputTableName);
  }

  private Map<TimelineClusterMetric, MetricHostAggregate> aggregateMetricsFromResultSet(ResultSet rs, long endTime)
    throws IOException, SQLException {

    TimelineClusterMetric existingMetric = null;
    MetricHostAggregate hostAggregate = null;
    Map<TimelineClusterMetric, MetricHostAggregate> hostAggregateMap =
      new HashMap<TimelineClusterMetric, MetricHostAggregate>();
    int perMetricCount = 0;

    while (rs.next()) {
      TimelineClusterMetric currentMetric = readHelper.fromResultSet(rs);
      if (currentMetric == null) {
        continue;
      }
      MetricClusterAggregate currentHostAggregate =
        isClusterPrecisionInputTable ?
          readHelper.getMetricClusterAggregateFromResultSet(rs) :
          readHelper.getMetricClusterTimeAggregateFromResultSet(rs);

      if (existingMetric == null) {
        // First row
        existingMetric = currentMetric;
        currentMetric.setTimestamp(endTime);
        hostAggregate = new MetricHostAggregate();
        hostAggregateMap.put(currentMetric, hostAggregate);
        perMetricCount++;
      }

      if (existingMetric.equalsExceptTime(currentMetric)) {
        // Recalculate totals with current metric
        updateAggregatesFromHost(hostAggregate, currentHostAggregate);
        perMetricCount++;
      } else {
        // Switched over to a new metric - save new metric

        hostAggregate.setSum(hostAggregate.getSum() / (perMetricCount - 1));
        hostAggregate.setNumberOfSamples(Math.round((float)hostAggregate.getNumberOfSamples() / (float)(perMetricCount - 1)));
        perMetricCount = 1;

        hostAggregate = new MetricHostAggregate();
        currentMetric.setTimestamp(endTime);
        updateAggregatesFromHost(hostAggregate, currentHostAggregate);
        hostAggregateMap.put(currentMetric, hostAggregate);
        existingMetric = currentMetric;
      }

    }

    if (existingMetric != null) {
      hostAggregate.setSum(hostAggregate.getSum() / perMetricCount);
      hostAggregate.setNumberOfSamples(Math.round((float)hostAggregate.getNumberOfSamples() / (float)perMetricCount));
    }

    return hostAggregateMap;
  }

  private void updateAggregatesFromHost(MetricHostAggregate agg, MetricClusterAggregate currentClusterAggregate) {
    agg.updateMax(currentClusterAggregate.getMax());
    agg.updateMin(currentClusterAggregate.getMin());
    agg.updateSum(currentClusterAggregate.getSum());
    agg.updateNumberOfSamples(currentClusterAggregate.getNumberOfHosts());
  }
}
