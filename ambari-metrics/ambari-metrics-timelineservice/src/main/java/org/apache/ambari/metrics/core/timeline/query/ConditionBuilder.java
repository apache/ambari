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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.ambari.metrics.core.timeline.aggregators.Function;

public class ConditionBuilder {

  private List<String> metricNames;
  private List<String> hostnames;
  private String appId;
  private String instanceId;
  private Long startTime;
  private Long endTime;
  private Precision precision;
  private Integer limit;
  private boolean grouped;
  private boolean noLimit = false;
  private Integer fetchSize;
  private String statement;
  private Set<String> orderByColumns = new LinkedHashSet<String>();
  private Integer topN;
  private boolean isBottomN;
  private Function topNFunction;
  private List<byte[]> uuids;
  private List<String> transientMetricNames;

  public ConditionBuilder(List<String> metricNames) {
    this.metricNames = metricNames;
  }

  public ConditionBuilder hostnames(List<String> hostnames) {
    this.hostnames = hostnames;
    return this;
  }

  public ConditionBuilder appId(String appId) {
    this.appId = appId;
    return this;
  }

  public ConditionBuilder instanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  public ConditionBuilder startTime(Long startTime) {
    this.startTime = startTime;
    return this;
  }

  public ConditionBuilder endTime(Long endTime) {
    this.endTime = endTime;
    return this;
  }

  public ConditionBuilder precision(Precision precision) {
    this.precision = precision;
    return this;
  }

  public ConditionBuilder limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public ConditionBuilder grouped(boolean grouped) {
    this.grouped = grouped;
    return this;
  }

  public ConditionBuilder noLimit(boolean noLimit) {
    this.noLimit = noLimit;
    return this;
  }

  public ConditionBuilder fetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
    return this;
  }

  public ConditionBuilder statement(String statement) {
    this.statement = statement;
    return this;
  }

  public ConditionBuilder orderByColumns(Set<String> orderByColumns) {
    this.orderByColumns = orderByColumns;
    return this;
  }

  public ConditionBuilder topN(Integer topN) {
    this.topN = topN;
    return this;
  }

  public ConditionBuilder isBottomN(boolean isBottomN) {
    this.isBottomN = isBottomN;
    return this;
  }

  public ConditionBuilder topNFunction(Function topNFunction) {
    this.topNFunction = topNFunction;
    return this;
  }

  public ConditionBuilder uuid(List<byte[]> uuids) {
    this.uuids = uuids;
    return this;
  }

  public ConditionBuilder transientMetricNames(List<String> transientMetricNames) {
    this.transientMetricNames = transientMetricNames;
    return this;
  }

  public Condition build() {
    if (topN == null) {
      if (CollectionUtils.isEmpty(transientMetricNames)) {
        return new DefaultCondition(
          uuids, metricNames,
          hostnames, appId, instanceId, startTime, endTime,
          precision, limit, grouped);
      } else {
        return new TransientMetricCondition(
          uuids, metricNames,
          hostnames, appId, instanceId, startTime, endTime,
          precision, limit, grouped, transientMetricNames);
      }
    } else {
      return new TopNCondition(uuids, metricNames, hostnames, appId, instanceId,
        startTime, endTime, precision, limit, grouped, topN, topNFunction, isBottomN);
    }
  }
}
