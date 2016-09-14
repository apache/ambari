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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.metrics2.sink.timeline.Precision;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DefaultCondition implements Condition {
  List<String> metricNames;
  List<String> hostnames;
  String appId;
  String instanceId;
  Long startTime;
  Long endTime;
  Precision precision;
  Integer limit;
  boolean grouped;
  boolean noLimit = false;
  Integer fetchSize;
  String statement;
  Set<String> orderByColumns = new LinkedHashSet<String>();
  boolean metricNamesNotCondition = false;

  private static final Log LOG = LogFactory.getLog(DefaultCondition.class);

  public DefaultCondition(List<String> metricNames, List<String> hostnames, String appId,
                          String instanceId, Long startTime, Long endTime, Precision precision,
                          Integer limit, boolean grouped) {
    this.metricNames = metricNames;
    this.hostnames = hostnames;
    this.appId = appId;
    this.instanceId = instanceId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.precision = precision;
    this.limit = limit;
    this.grouped = grouped;
  }

  public String getStatement() {
    return statement;
  }

  public void setStatement(String statement) {
    this.statement = statement;
  }

  public List<String> getMetricNames() {
    return metricNames == null || metricNames.isEmpty() ? null : metricNames;
  }

  public StringBuilder getConditionClause() {
    StringBuilder sb = new StringBuilder();

    boolean appendConjunction = appendMetricNameClause(sb);

    appendConjunction = appendHostnameClause(sb, appendConjunction);

    appendConjunction = append(sb, appendConjunction, getAppId(), " APP_ID = ?");
    appendConjunction = append(sb, appendConjunction, getInstanceId(), " INSTANCE_ID = ?");
    appendConjunction = append(sb, appendConjunction, getStartTime(), " SERVER_TIME >= ?");
    append(sb, appendConjunction, getEndTime(), " SERVER_TIME < ?");

    return sb;
  }

  protected static boolean append(StringBuilder sb,
                                  boolean appendConjunction,
                                  Object value, String str) {
    if (value != null) {
      if (appendConjunction) {
        sb.append(" AND");
      }

      sb.append(str);
      appendConjunction = true;
    }
    return appendConjunction;
  }

  public List<String> getHostnames() {
    return hostnames;
  }

  public Precision getPrecision() {
    return precision;
  }

  public void setPrecision(Precision precision) {
    this.precision = precision;
  }

  public String getAppId() {
    if (appId != null && !appId.isEmpty()) {
      if (!(appId.equals("HOST") || appId.equals("FLUME_HANDLER"))) {
        return appId.toLowerCase();
      } else {
        return appId;
      }
    }
    return null;
  }

  public String getInstanceId() {
    return instanceId == null || instanceId.isEmpty() ? null : instanceId;
  }

  /**
   * Convert to millis.
   */
  public Long getStartTime() {
    if (startTime == null) {
      return null;
    } else if (startTime < 9999999999l) {
      return startTime * 1000;
    } else {
      return startTime;
    }
  }

  public Long getEndTime() {
    if (endTime == null) {
      return null;
    }
    if (endTime < 9999999999l) {
      return endTime * 1000;
    } else {
      return endTime;
    }
  }

  public void setNoLimit() {
    this.noLimit = true;
  }

  @Override
  public boolean doUpdate() {
    return false;
  }

  public Integer getLimit() {
    if (noLimit) {
      return null;
    }
    return limit == null ? PhoenixHBaseAccessor.RESULTSET_LIMIT : limit;
  }

  public boolean isGrouped() {
    return grouped;
  }

  public boolean isPointInTime() {
    return getStartTime() == null && getEndTime() == null;
  }

  public boolean isEmpty() {
    return (metricNames == null || metricNames.isEmpty())
      && (hostnames == null || hostnames.isEmpty())
      && (appId == null || appId.isEmpty())
      && (instanceId == null || instanceId.isEmpty())
      && startTime == null
      && endTime == null;
  }

  public Integer getFetchSize() {
    return fetchSize;
  }

  public void setFetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
  }

  public void addOrderByColumn(String column) {
    orderByColumns.add(column);
  }

  public String getOrderByClause(boolean asc) {
    String orderByStr = " ORDER BY ";
    if (!orderByColumns.isEmpty()) {
      StringBuilder sb = new StringBuilder(orderByStr);
      for (String orderByColumn : orderByColumns) {
        if (sb.length() != orderByStr.length()) {
          sb.append(", ");
        }
        sb.append(orderByColumn);
        if (!asc) {
          sb.append(" DESC");
        }
      }
      sb.append(" ");
      return sb.toString();
    }
    return null;
  }

  protected boolean appendMetricNameClause(StringBuilder sb) {
    boolean appendConjunction = false;
    List<String> metricsLike = new ArrayList<>();
    List<String> metricsIn = new ArrayList<>();

    if (getMetricNames() != null) {
      for (String name : getMetricNames()) {
        if (name.contains("%")) {
          metricsLike.add(name);
        } else {
          metricsIn.add(name);
        }
      }

      // Put a '(' first
      sb.append("(");

      //IN clause
      // METRIC_NAME (NOT) IN (?,?,?,?)
      if (CollectionUtils.isNotEmpty(metricsIn)) {
        sb.append("METRIC_NAME");
        if (metricNamesNotCondition) {
          sb.append(" NOT");
        }
        sb.append(" IN (");
        //Append ?,?,?,?
        for (int i = 0; i < metricsIn.size(); i++) {
          sb.append("?");
          if (i < metricsIn.size() - 1) {
            sb.append(", ");
          }
        }
        sb.append(")");
        appendConjunction = true;
      }

      //Put an OR/AND if both types are present
      if (CollectionUtils.isNotEmpty(metricsIn) &&
        CollectionUtils.isNotEmpty(metricsLike)) {
        if (metricNamesNotCondition) {
          sb.append(" AND ");
        } else {
          sb.append(" OR ");
        }
      }

      //LIKE clause
      // METRIC_NAME (NOT) LIKE ? OR(AND) METRIC_NAME LIKE ?
      if (CollectionUtils.isNotEmpty(metricsLike)) {

        for (int i = 0; i < metricsLike.size(); i++) {
          sb.append("METRIC_NAME");
          if (metricNamesNotCondition) {
            sb.append(" NOT");
          }
          sb.append(" LIKE ");
          sb.append("?");

          if (i < metricsLike.size() - 1) {
            if (metricNamesNotCondition) {
              sb.append(" AND ");
            } else {
              sb.append(" OR ");
            }
          }
        }
        appendConjunction = true;
      }

      // Finish with a ')'
      if (appendConjunction) {
        sb.append(")");
      }

      metricNames.clear();
      if (CollectionUtils.isNotEmpty(metricsIn)) {
        metricNames.addAll(metricsIn);
      }
      if (CollectionUtils.isNotEmpty(metricsLike)) {
        metricNames.addAll(metricsLike);
      }
    }
    return appendConjunction;
  }

  protected boolean appendHostnameClause(StringBuilder sb, boolean appendConjunction) {
    boolean hostnameContainsRegex = false;
    if (hostnames != null) {
      for (String hostname : hostnames) {
        if (hostname.contains("%")) {
          hostnameContainsRegex = true;
          break;
        }
      }
    }

    StringBuilder hostnamesCondition = new StringBuilder();
    if (hostnameContainsRegex) {
      hostnamesCondition.append(" (");
      for (String hostname : getHostnames()) {
        if (hostnamesCondition.length() > 2) {
          hostnamesCondition.append(" OR ");
        }
        hostnamesCondition.append("HOSTNAME LIKE ?");
      }
      hostnamesCondition.append(")");

      appendConjunction = append(sb, appendConjunction, getHostnames(), hostnamesCondition.toString());
    } else if (hostnames != null && getHostnames().size() > 1) {
      for (String hostname : getHostnames()) {
        if (hostnamesCondition.length() > 0) {
          hostnamesCondition.append(" ,");
        } else {
          hostnamesCondition.append(" HOSTNAME IN (");
        }
        hostnamesCondition.append('?');
      }
      hostnamesCondition.append(')');
      appendConjunction = append(sb, appendConjunction, getHostnames(), hostnamesCondition.toString());

    } else {
      appendConjunction = append(sb, appendConjunction, getHostnames(), " HOSTNAME = ?");
    }
    return appendConjunction;
  }

  @Override
  public String toString() {
    return "Condition{" +
      "metricNames=" + metricNames +
      ", hostnames='" + hostnames + '\'' +
      ", appId='" + appId + '\'' +
      ", instanceId='" + instanceId + '\'' +
      ", startTime=" + startTime +
      ", endTime=" + endTime +
      ", limit=" + limit +
      ", grouped=" + grouped +
      ", orderBy=" + orderByColumns +
      ", noLimit=" + noLimit +
      '}';
  }

  protected static boolean metricNamesHaveWildcard(List<String> metricNames) {
    for (String name : metricNames) {
      if (name.contains("%")) {
        return true;
      }
    }
    return false;
  }

  protected static boolean hostNamesHaveWildcard(List<String> hostnames) {
    if (hostnames == null)
      return false;
    for (String name : hostnames) {
      if (name.contains("%")) {
        return true;
      }
    }
    return false;
  }

  public void setMetricNamesNotCondition(boolean metricNamesNotCondition) {
    this.metricNamesNotCondition = metricNamesNotCondition;
  }
}
