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
package org.apache.ambari.metrics.core.timeline.query;

import java.util.List;

import org.apache.ambari.metrics.core.timeline.aggregators.Function;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.Precision;

public class TopNCondition extends DefaultCondition{

  private Integer topN;
  private boolean isBottomN;
  private Function topNFunction;
  private static final Log LOG = LogFactory.getLog(TopNCondition.class);

  public TopNCondition(List<byte[]> uuids, List<String> metricNames, List<String> hostnames, String appId,
                          String instanceId, Long startTime, Long endTime, Precision precision,
                          Integer limit, boolean grouped, Integer topN, Function topNFunction,
                          boolean isBottomN) {
    super(uuids, metricNames, hostnames, appId, instanceId, startTime, endTime, precision, limit, grouped);
    this.topN = topN;
    this.isBottomN = isBottomN;
    this.topNFunction = topNFunction;
  }

  @Override
  public StringBuilder getConditionClause() {


    if (!(isTopNHostCondition(metricNames, hostnames) || isTopNMetricCondition(metricNames, hostnames))) {
      LOG.error("Unsupported TopN Operation requested. Query can have either multiple hosts or multiple metric names " +
        "but not both.");
      return null;
    }

    StringBuilder sb = new StringBuilder();
    sb.append(" UUID IN (");
    sb.append(getTopNInnerQuery());
    sb.append(")");

    boolean appendConjunction = true;
    appendConjunction = append(sb, appendConjunction, getStartTime(), " SERVER_TIME >= ?");
    append(sb, appendConjunction, getEndTime(), " SERVER_TIME < ?");

    return sb;
  }

  public String getTopNInnerQuery() {
    return String.format(PhoenixTransactSQL.TOP_N_INNER_SQL,
      PhoenixTransactSQL.getTargetTableUsingPrecision(precision, CollectionUtils.isNotEmpty(hostnames)),
      super.getConditionClause().toString(), getTopNOrderByClause(), topN);
  }

  private String getTopNOrderByClause() {

    String orderByClause = getColumnSelect(this.topNFunction);
    orderByClause += (isBottomN ? " ASC" : " DESC");
    return  orderByClause;
  }

  public static String getColumnSelect(Function topNFunction) {
    String columnSelect = null;
    if (topNFunction != null) {
      switch (topNFunction.getReadFunction()) {
        case AVG:
          columnSelect = "ROUND(AVG(METRIC_SUM),2)";
          break;
        case SUM:
          columnSelect = "SUM(METRIC_SUM)";
          break;
        default:
          columnSelect = "MAX(METRIC_MAX)";
          break;
      }
    }
    if (columnSelect == null) {
      columnSelect = "MAX(METRIC_MAX)";
    }
    return  columnSelect;
  }

  public boolean isTopNHostCondition() {
    return isTopNHostCondition(metricNames, hostnames);
  }

  public boolean isTopNMetricCondition() {
    return isTopNMetricCondition(metricNames, hostnames);
  }

  /**
   * Check if this is a case of Top N hosts condition
   * @param metricNames A list of Strings.
   * @param hostnames A list of Strings.
   * @return True if it is a Case of Top N Hosts (1 Metric and H hosts).
   */
  public static boolean isTopNHostCondition(List<String> metricNames, List<String> hostnames) {
    // Case 1 : 1 Metric, H hosts
    // Select Top N or Bottom N host series based on 1 metric (max/avg/sum)
    // Hostnames cannot be empty
    // Only 1 metric allowed, without wildcards
    return (CollectionUtils.isNotEmpty(hostnames) && metricNames.size() == 1 && !metricNamesHaveWildcard(metricNames));

  }

  /**
   * Check if this is a case of Top N metrics condition
   * @param metricNames A list of Strings.
   * @param hostnames A list of Strings.
   * @return True if it is a Case of Top N Metrics (M Metric and 1 or 0 host).
   */
  public static boolean isTopNMetricCondition(List<String> metricNames, List<String> hostnames) {
    // Case 2 : M Metric names or Regex, 1 or No host
    // Select Top N or Bottom N metric series based on metric values(max/avg/sum)
    // MetricNames cannot be empty
    // No host (aggregate) or 1 host allowed, without wildcards
    return (CollectionUtils.isNotEmpty(metricNames) && (hostnames == null || hostnames.size() <= 1) &&
      !hostNamesHaveWildcard(hostnames));
  }

  public Integer getTopN() {
    return topN;
  }

  public void setTopN(Integer topN) {
    this.topN = topN;
  }

  public boolean isBottomN() {
    return isBottomN;
  }

  public void setIsBottomN(boolean isBottomN) {
    this.isBottomN = isBottomN;
  }

  public Function getTopNFunction() {
    return topNFunction;
  }

  public void setTopNFunction(Function topNFunction) {
    this.topNFunction = topNFunction;
  }
}
