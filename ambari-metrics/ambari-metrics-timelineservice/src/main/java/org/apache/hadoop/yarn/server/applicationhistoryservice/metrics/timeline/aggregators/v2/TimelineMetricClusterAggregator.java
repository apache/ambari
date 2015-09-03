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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AbstractTimelineAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricReadHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.EmptyCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_AGGREGATED_APP_METRIC_GROUPBY_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

public class TimelineMetricClusterAggregator extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricClusterAggregator.class);
  private final String aggregateColumnName;

  public TimelineMetricClusterAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                         Configuration metricsConf,
                                         String checkpointLocation,
                                         Long sleepIntervalMillis,
                                         Integer checkpointCutOffMultiplier,
                                         String hostAggregatorDisabledParam,
                                         String inputTableName,
                                         String outputTableName,
                                         Long nativeTimeRangeDelay) {
    super(hBaseAccessor, metricsConf, checkpointLocation,
      sleepIntervalMillis, checkpointCutOffMultiplier,
      hostAggregatorDisabledParam, inputTableName, outputTableName,
      nativeTimeRangeDelay);

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
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, nativeTimeRangeDelay),
      outputTableName, aggregateColumnName, tableName,
      startTime, endTime));

    return condition;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws IOException, SQLException {
    LOG.info("Aggregated cluster metrics for " + outputTableName +
      ", with startTime = " + new Date(startTime) +
      ", endTime = " + new Date(endTime));
  }
}
