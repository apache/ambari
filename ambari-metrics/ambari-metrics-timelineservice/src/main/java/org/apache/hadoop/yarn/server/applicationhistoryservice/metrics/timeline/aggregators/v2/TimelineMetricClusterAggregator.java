/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.v2;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_AGGREGATED_APP_METRIC_GROUPBY_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AbstractTimelineAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.EmptyCondition;

public class TimelineMetricClusterAggregator extends AbstractTimelineAggregator {
  private final String aggregateColumnName;

  public TimelineMetricClusterAggregator(AGGREGATOR_NAME aggregatorName,
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

    if (inputTableName.equals(METRICS_CLUSTER_AGGREGATE_TABLE_NAME)) {
      aggregateColumnName = "HOSTS_COUNT";
    } else {
      aggregateColumnName = "METRIC_COUNT";
    }
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    EmptyCondition condition = new EmptyCondition();
    condition.setDoUpdate(true);

    /*
    UPSERT INTO METRIC_AGGREGATE_HOURLY (METRIC_NAME, APP_ID, INSTANCE_ID,
    SERVER_TIME, UNITS, METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN)
    SELECT METRIC_NAME, APP_ID, INSTANCE_ID, MAX(SERVER_TIME), UNITS,
    SUM(METRIC_SUM), SUM(HOSTS_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN)
    FROM METRIC_AGGREGATE WHERE SERVER_TIME >= 1441155600000 AND
    SERVER_TIME < 1441159200000 GROUP BY METRIC_NAME, APP_ID, INSTANCE_ID, UNITS;
     */

    condition.setStatement(String.format(GET_AGGREGATED_APP_METRIC_GROUPBY_SQL,
      outputTableName, endTime, aggregateColumnName, tableName,
      getDownsampledMetricSkipClause(), startTime, endTime));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Condition: " + condition.toString());
    }

    return condition;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws IOException, SQLException {
    LOG.info("Aggregated cluster metrics for " + outputTableName +
      ", with startTime = " + new Date(startTime) +
      ", endTime = " + new Date(endTime));
  }
}
