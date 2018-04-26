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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.EVENT_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.EVENT_DOWNSAMPLER_HOST_METRIC_SELECT_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;

public class EventMetricDownSampler implements CustomDownSampler{

  private String metricPatterns = "";
  private static final Log LOG = LogFactory.getLog(EventMetricDownSampler.class);

  public static EventMetricDownSampler fromConfig(Map<String, String> conf) {
    String metricPatterns = conf.get(DownSamplerUtils.downSamplerConfigPrefix + DownSamplerUtils.eventDownSamplerKey + "." +
      DownSamplerUtils.downSamplerMetricPatternsConfig);

    return new EventMetricDownSampler(metricPatterns);
  }

  public EventMetricDownSampler(String metricPatterns) {
    this.metricPatterns = metricPatterns;
  }

  @Override
  public boolean validateConfigs() {
    return true;
  }

  @Override
  public List<String> prepareDownSamplingStatement(Long startTime, Long endTime, String tableName) {
    List<String> stmts = new ArrayList<>();
    List<String> metricPatternList = Arrays.asList(metricPatterns.split(","));

    String aggregateColumnName = "METRIC_COUNT";

    if (tableName.equals(METRICS_CLUSTER_AGGREGATE_TABLE_NAME)) {
      aggregateColumnName = "HOSTS_COUNT";
    }

    for (String metricPattern : metricPatternList) {
      String metricPatternClause = "'" + metricPattern + "'";
      if (tableName.contains("RECORD")) {
        stmts.add(String.format(EVENT_DOWNSAMPLER_HOST_METRIC_SELECT_SQL,
          endTime, tableName, metricPatternClause,
          startTime, endTime));
      } else {
        stmts.add(String.format(EVENT_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL,
          endTime, aggregateColumnName, tableName, metricPatternClause,
          startTime, endTime));
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Downsampling Stmt: " + stmts.toString());
    }
    return stmts;
  }
}