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
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.Condition;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.GET_METRIC_AGGREGATE_ONLY_SQL;

public class TimelineMetricAggregator extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog
    (TimelineMetricAggregator.class);

  private final String checkpointLocation;
  private final Long sleepIntervalMillis;
  private final Integer checkpointCutOffMultiplier;
  private final String hostAggregatorDisabledParam;
  private final String tableName;
  private final String outputTableName;
  private final Long nativeTimeRangeDelay;

  public TimelineMetricAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                  Configuration metricsConf,
                                  String checkpointLocation,
                                  Long sleepIntervalMillis,
                                  Integer checkpointCutOffMultiplier,
                                  String hostAggregatorDisabledParam,
                                  String tableName,
                                  String outputTableName,
                                  Long nativeTimeRangeDelay) {
    super(hBaseAccessor, metricsConf);
    this.checkpointLocation = checkpointLocation;
    this.sleepIntervalMillis = sleepIntervalMillis;
    this.checkpointCutOffMultiplier = checkpointCutOffMultiplier;
    this.hostAggregatorDisabledParam = hostAggregatorDisabledParam;
    this.tableName = tableName;
    this.outputTableName = outputTableName;
    this.nativeTimeRangeDelay =  nativeTimeRangeDelay;
  }

  @Override
  protected String getCheckpointLocation() {
    return checkpointLocation;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime)
    throws IOException, SQLException {
    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap =
      aggregateMetricsFromResultSet(rs);

    LOG.info("Saving " + hostAggregateMap.size() + " metric aggregates.");
    hBaseAccessor.saveHostAggregateRecords(hostAggregateMap,
      outputTableName);
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    Condition condition = new Condition(null, null, null, null, startTime,
      endTime, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, nativeTimeRangeDelay),
      tableName));
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("HOSTNAME");
    condition.addOrderByColumn("APP_ID");
    condition.addOrderByColumn("INSTANCE_ID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  private Map<TimelineMetric, MetricHostAggregate> aggregateMetricsFromResultSet
      (ResultSet rs) throws IOException, SQLException {
    TimelineMetric existingMetric = null;
    MetricHostAggregate hostAggregate = null;
    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap =
      new HashMap<TimelineMetric, MetricHostAggregate>();

    while (rs.next()) {
      TimelineMetric currentMetric =
        PhoenixHBaseAccessor.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        PhoenixHBaseAccessor.getMetricHostAggregateFromResultSet(rs);

      if (existingMetric == null) {
        // First row
        existingMetric = currentMetric;
        hostAggregate = new MetricHostAggregate();
        hostAggregateMap.put(currentMetric, hostAggregate);
      }

      if (existingMetric.equalsExceptTime(currentMetric)) {
        // Recalculate totals with current metric
        hostAggregate.updateAggregates(currentHostAggregate);
      } else {
        // Switched over to a new metric - save existing - create new aggregate
        hostAggregate = new MetricHostAggregate();
        hostAggregate.updateAggregates(currentHostAggregate);
        hostAggregateMap.put(currentMetric, hostAggregate);
        existingMetric = currentMetric;
      }
    }
    return hostAggregateMap;
  }

  @Override
  protected Long getSleepIntervalMillis() {
    return sleepIntervalMillis;
  }

  @Override
  protected Integer getCheckpointCutOffMultiplier() {
    return checkpointCutOffMultiplier;
  }

  @Override
  protected boolean isDisabled() {
    return metricsConf.getBoolean(hostAggregatorDisabledParam, false);
  }
}
