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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AbstractTimelineAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.EmptyCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL;

public class TimelineMetricHostAggregator extends AbstractTimelineAggregator {

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

    LOG.info("Aggregated host metrics for " + outputTableName +
      ", with startTime = " + new Date(startTime) +
      ", endTime = " + new Date(endTime));
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    EmptyCondition condition = new EmptyCondition();
    condition.setDoUpdate(true);

    condition.setStatement(String.format(GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, nativeTimeRangeDelay),
      outputTableName, endTime, tableName, startTime, endTime));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Condition: " + condition.toString());
    }

    return condition;
  }
}