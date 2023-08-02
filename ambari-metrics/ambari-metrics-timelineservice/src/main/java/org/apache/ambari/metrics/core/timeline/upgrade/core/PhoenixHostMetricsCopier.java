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
package org.apache.ambari.metrics.core.timeline.upgrade.core;

import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;


import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhoenixHostMetricsCopier extends AbstractPhoenixMetricsCopier {
  private static final Log LOG = LogFactory.getLog(PhoenixHostMetricsCopier.class);
  private final Map<TimelineMetric, MetricHostAggregate> aggregateMap = new HashMap<>();

  PhoenixHostMetricsCopier(String inputTableName, String outputTableName, PhoenixHBaseAccessor hBaseAccessor, Set<String> metricNames, long startTime, Writer processedMetricsFileWriter) {
    super(inputTableName, outputTableName, hBaseAccessor, metricNames, startTime, processedMetricsFileWriter);
  }

  @Override
  protected String getColumnsClause() {
    return "METRIC_NAME, " +
      "HOSTNAME, " +
      "APP_ID, " +
      "INSTANCE_ID, " +
      "SERVER_TIME, " +
      "METRIC_SUM, " +
      "METRIC_COUNT, " +
      "METRIC_MAX, " +
      "METRIC_MIN";
  }

  @Override
  protected void saveMetrics() throws SQLException {
    LOG.debug(String.format("Saving %s results read from %s into %s", aggregateMap.size(), inputTable, outputTable));
    this.hBaseAccessor.saveHostAggregateRecords(aggregateMap, outputTable);
  }

  @Override
  protected void addToResults(ResultSet rs) throws SQLException {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(rs.getString("METRIC_NAME"));
    timelineMetric.setHostName(rs.getString("HOSTNAME"));
    timelineMetric.setAppId(rs.getString("APP_ID"));
    timelineMetric.setInstanceId(rs.getString("INSTANCE_ID"));
    timelineMetric.setStartTime(rs.getLong("SERVER_TIME"));

    MetricHostAggregate metricHostAggregate = extractMetricHostAggregate(rs);

    this.aggregateMap.put(timelineMetric, metricHostAggregate);
  }
}
