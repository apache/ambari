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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_AGGREGATE_ONLY_SQL;

public class TimelineMetricHostAggregator extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricHostAggregator.class);
  TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

  public TimelineMetricHostAggregator(String aggregatorName,
                                      PhoenixHBaseAccessor hBaseAccessor,
                                      Configuration metricsConf,
                                      String checkpointLocation,
                                      Long sleepIntervalMillis,
                                      Integer checkpointCutOffMultiplier,
                                      String hostAggregatorDisabledParam,
                                      String tableName,
                                      String outputTableName,
                                      Long nativeTimeRangeDelay) {
    super(aggregatorName, hBaseAccessor, metricsConf, checkpointLocation,
      sleepIntervalMillis, checkpointCutOffMultiplier, hostAggregatorDisabledParam,
      tableName, outputTableName, nativeTimeRangeDelay);
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws IOException, SQLException {

    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap = aggregateMetricsFromResultSet(rs, endTime);

    LOG.info("Saving " + hostAggregateMap.size() + " metric aggregates.");
    hBaseAccessor.saveHostAggregateRecords(hostAggregateMap, outputTableName);
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, nativeTimeRangeDelay),
      tableName));
    // Retaining order of the row-key avoids client side merge sort.
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("HOSTNAME");
    condition.addOrderByColumn("SERVER_TIME");
    condition.addOrderByColumn("APP_ID");
    condition.addOrderByColumn("INSTANCE_ID");
    return condition;
  }

  private Map<TimelineMetric, MetricHostAggregate> aggregateMetricsFromResultSet(ResultSet rs, long endTime)
      throws IOException, SQLException {
    TimelineMetric existingMetric = null;
    MetricHostAggregate hostAggregate = null;
    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap =  new HashMap<TimelineMetric, MetricHostAggregate>();


    while (rs.next()) {
      TimelineMetric currentMetric =
        readHelper.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        readHelper.getMetricHostAggregateFromResultSet(rs);

      if (existingMetric == null) {
        // First row
        existingMetric = currentMetric;
        currentMetric.setTimestamp(endTime);
        hostAggregate = new MetricHostAggregate();
        hostAggregateMap.put(currentMetric, hostAggregate);
      }

      if (existingMetric.equalsExceptTime(currentMetric)) {
        // Recalculate totals with current metric
        hostAggregate.updateAggregates(currentHostAggregate);
      } else {
        // Switched over to a new metric - save existing - create new aggregate
        currentMetric.setTimestamp(endTime);
        hostAggregate = new MetricHostAggregate();
        hostAggregate.updateAggregates(currentHostAggregate);
        hostAggregateMap.put(currentMetric, hostAggregate);
        existingMetric = currentMetric;
      }
    }
    return hostAggregateMap;
  }


}
