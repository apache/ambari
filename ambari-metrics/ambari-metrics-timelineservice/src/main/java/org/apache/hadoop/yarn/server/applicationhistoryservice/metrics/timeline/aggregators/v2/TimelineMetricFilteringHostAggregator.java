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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.v2;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.EmptyCondition;

public class TimelineMetricFilteringHostAggregator extends TimelineMetricHostAggregator {
  private TimelineMetricMetadataManager metricMetadataManager;
  private ConcurrentHashMap<String, Long> postedAggregatedMap;

  public TimelineMetricFilteringHostAggregator(AggregationTaskRunner.AGGREGATOR_NAME aggregatorName,
                                               TimelineMetricMetadataManager metricMetadataManager,
                                               PhoenixHBaseAccessor hBaseAccessor,
                                               Configuration metricsConf,
                                               String checkpointLocation,
                                               Long sleepIntervalMillis,
                                               Integer checkpointCutOffMultiplier,
                                               String hostAggregatorDisabledParam,
                                               String tableName,
                                               String outputTableName,
                                               Long nativeTimeRangeDelay,
                                               MetricCollectorHAController haController,
                                               ConcurrentHashMap<String, Long> postedAggregatedMap) {
    super(aggregatorName,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      tableName,
      outputTableName,
      nativeTimeRangeDelay,
      haController);
    this.metricMetadataManager = metricMetadataManager;
    this.postedAggregatedMap = postedAggregatedMap;
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    List<String> aggregatedHostnames = new ArrayList<>();
    for (Map.Entry<String, Long> entry : postedAggregatedMap.entrySet()) {
      if (entry.getValue() > startTime && entry.getValue() <= endTime) {
        aggregatedHostnames.add(entry.getKey());
      }
    }
    List<String> notAggregatedHostnames = metricMetadataManager.getNotLikeHostnames(aggregatedHostnames);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Already aggregated hostnames based on postedAggregatedMap : " + aggregatedHostnames);
      LOG.debug("Hostnames that will be aggregated : " + notAggregatedHostnames);
    }
    List<byte[]> uuids = metricMetadataManager.getUuids(new ArrayList<String>(), notAggregatedHostnames, "", "");

    EmptyCondition condition = new EmptyCondition();
    condition.setDoUpdate(true);

    condition.setStatement(String.format(GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL,
      outputTableName, endTime, tableName,
      getDownsampledMetricSkipClause() + getIncludedUuidsClause(uuids), startTime, endTime));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Condition: " + condition.toString());
    }

    return condition;
  }

  private String getIncludedUuidsClause(List<byte[]> uuids) {
    StringBuilder sb = new StringBuilder();
    sb.append("(");

    //LIKE clause
    // (UUID LIKE ? OR UUID LIKE ?) AND
    if (CollectionUtils.isNotEmpty(uuids)) {
      for (int i = 0; i < uuids.size(); i++) {
        sb.append("UUID");
        sb.append(" LIKE ");
        sb.append("'%");
        sb.append(new String(uuids.get(i)));
        sb.append("'");

        if (i == uuids.size() - 1) {
          sb.append(") AND ");
        } else {
          sb.append(" OR ");
        }
      }
    }
    return sb.toString();
  }
}
