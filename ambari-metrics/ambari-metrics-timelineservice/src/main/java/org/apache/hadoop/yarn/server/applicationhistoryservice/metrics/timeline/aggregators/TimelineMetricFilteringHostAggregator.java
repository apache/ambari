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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_AGGREGATE_ONLY_SQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;

public class TimelineMetricFilteringHostAggregator extends TimelineMetricHostAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricFilteringHostAggregator.class);
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
    super(aggregatorName, metricMetadataManager,
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

    Condition condition = new DefaultCondition(uuids, null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL, tableName));
    // Retaining order of the row-key avoids client side merge sort.
    condition.addOrderByColumn("UUID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }
}
