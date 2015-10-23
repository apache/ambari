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
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.metrics2.sink.timeline.Precision;

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
    boolean appendConjunction = false;
    StringBuilder metricsLike = new StringBuilder();
    StringBuilder metricsIn = new StringBuilder();

    if (getMetricNames() != null) {
      for (String name : getMetricNames()) {
        if (name.contains("%")) {
          if (metricsLike.length() > 1) {
            metricsLike.append(" OR ");
          }
          metricsLike.append("METRIC_NAME LIKE ?");
        } else {
          if (metricsIn.length() > 0) {
            metricsIn.append(", ");
          }
          metricsIn.append("?");
        }
      }

      if (metricsIn.length()>0) {
        sb.append("(METRIC_NAME IN (");
        sb.append(metricsIn);
        sb.append(")");
        appendConjunction = true;
      }

      if (metricsLike.length() > 0) {
        if (appendConjunction) {
          sb.append(" OR ");
        } else {
          sb.append("(");
        }
        sb.append(metricsLike);
        appendConjunction = true;
      }

      if (appendConjunction) {
        sb.append(")");
      }
    }

    if (hostnames != null && getHostnames().size() > 1) {
      StringBuilder hostnamesCondition = new StringBuilder();
      for (String hostname: getHostnames()) {
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
      if (!(appId.equals("HOST") || appId.equals("FLUME_HANDLER")) ) {
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
}
