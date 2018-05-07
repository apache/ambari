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
package org.apache.ambari.metrics.core.timeline;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhoenixClusterMetricsCopier extends AbstractPhoenixMetricsCopier {
  private Map<TimelineClusterMetric, MetricHostAggregate> aggregateMap = new HashMap<>();

  PhoenixClusterMetricsCopier(String inputTableName, String outputTableName, PhoenixHBaseAccessor hBaseAccessor, Set<String> metricNames) {
    super(inputTableName, outputTableName, hBaseAccessor, metricNames);
  }

  @Override
  protected void saveResults() throws SQLException {
    System.out.println(String.format("Saving %s results", aggregateMap.size()));
    hBaseAccessor.saveClusterAggregateRecordsSecond(aggregateMap, outputTable);
  }

  @Override
  protected void addToResults(ResultSet rs) throws SQLException {
    TimelineClusterMetric timelineMetric = new TimelineClusterMetric(
            rs.getString("METRIC_NAME"), rs.getString("APP_ID"),
            rs.getString("INSTANCE_ID"), rs.getLong("SERVER_TIME"));

    MetricHostAggregate metricHostAggregate = new MetricHostAggregate();
    metricHostAggregate.setMin(rs.getDouble("METRIC_MIN"));
    metricHostAggregate.setMax(rs.getDouble("METRIC_MAX"));
    metricHostAggregate.setSum(rs.getDouble("METRIC_SUM"));
    metricHostAggregate.setNumberOfSamples(rs.getLong("METRIC_COUNT"));

    aggregateMap.put(timelineMetric, metricHostAggregate);

  }
}
