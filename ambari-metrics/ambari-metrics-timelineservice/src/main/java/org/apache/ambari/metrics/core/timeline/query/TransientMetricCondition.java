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

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.metrics2.sink.timeline.Precision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to encapsulate condition to query transient metrics.
 */
public class TransientMetricCondition extends DefaultCondition {

  private List<String> transientMetricNames = new ArrayList<>();

  public TransientMetricCondition(List<byte[]> uuids, List<String> metricNames, List<String> hostnames, String appId,
                          String instanceId, Long startTime, Long endTime, Precision precision,
                          Integer limit, boolean grouped, List<String> transientMetricNames) {
    super(uuids, metricNames, hostnames, appId, instanceId, startTime, endTime, precision, limit, grouped);
    this.transientMetricNames = transientMetricNames;
    if (CollectionUtils.isEmpty(hostnames)) {
      this.hostnames = Collections.singletonList("%");
    }
  }

  public TransientMetricCondition(List<String> metricNames, List<String> hostnames, String appId,
                                  String instanceId, Long startTime, Long endTime, Precision precision,
                                  Integer limit, boolean grouped, List<String> transientMetricNames) {
    super(metricNames, hostnames, appId, instanceId, startTime, endTime, precision, limit, grouped);
    this.transientMetricNames = transientMetricNames;
    if (CollectionUtils.isEmpty(hostnames)) {
      this.hostnames = Collections.singletonList("%");
    }
  }


  public StringBuilder getTransientConditionClause() {
    StringBuilder sb = new StringBuilder();

    boolean appendConjunction = appendMetricNameClause(sb);

    appendConjunction = appendHostnameClause(sb, appendConjunction);

    String appId = getAppId();
    if (appId.contains("%")) {
      appendConjunction = append(sb, appendConjunction, getAppId(), " APP_ID LIKE ?");
    } else {
      appendConjunction = append(sb, appendConjunction, getAppId(), " APP_ID = ?");
    }

    String instanceId = getInstanceId();
    if (instanceId.contains("%")) {
      appendConjunction = append(sb, appendConjunction, getInstanceId(), " INSTANCE_ID LIKE ?");
    } else {
      appendConjunction = append(sb, appendConjunction, getInstanceId(), " INSTANCE_ID = ?");
    }

    appendConjunction = append(sb, appendConjunction, getStartTime(), " SERVER_TIME >= ?");
    append(sb, appendConjunction, getEndTime(), " SERVER_TIME < ?");

    return sb;
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
    } else if (CollectionUtils.isNotEmpty(hostnames)) {
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

  @Override
  public List<String> getTransientMetricNames() {
    return transientMetricNames;
  }
}
