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

import java.util.Collections;
import java.util.List;

import org.apache.hadoop.metrics2.sink.timeline.Precision;
// TODO get rid of this class
public class SplitByMetricNamesCondition implements Condition {
  private final Condition adaptee;
  private byte[] currentUuid;
  private boolean metricNamesNotCondition = false;

  public SplitByMetricNamesCondition(Condition condition){
    this.adaptee = condition;
  }

  @Override
  public boolean isEmpty() {
    return adaptee.isEmpty();
  }

  @Override
  public List<byte[]> getUuids() {
    return adaptee.getUuids();
  }

  @Override
  public List<String> getMetricNames() {
    return Collections.singletonList(new String(currentUuid));
  }

  @Override
  public List<String> getTransientMetricNames() {
    return null;
  }

  @Override
  public boolean isPointInTime() {
    return adaptee.isPointInTime();
  }

  @Override
  public boolean isGrouped() {
    return adaptee.isGrouped();
  }

  @Override
  public void setStatement(String statement) {
    adaptee.setStatement(statement);
  }

  @Override
  public List<String> getHostnames() {
    return adaptee.getHostnames();
  }

  @Override
  public Precision getPrecision() {
    return adaptee.getPrecision();
  }

  @Override
  public void setPrecision(Precision precision) {
    adaptee.setPrecision(precision);
  }

  @Override
  public String getAppId() {
    return adaptee.getAppId();
  }

  @Override
  public String getInstanceId() {
    return adaptee.getInstanceId();
  }

  @Override
  public StringBuilder getConditionClause() {
    StringBuilder sb = new StringBuilder();
    boolean appendConjunction = false;

    if (getMetricNames() != null) {
      for (String name : getMetricNames()) {
        if (sb.length() > 1) {
          sb.append(" OR ");
        }
        sb.append("UUID = ?");
      }

      appendConjunction = true;
    }

    appendConjunction = DefaultCondition.append(sb, appendConjunction,
      getStartTime(), " SERVER_TIME >= ?");
    DefaultCondition.append(sb, appendConjunction, getEndTime(),
      " SERVER_TIME < ?");

    return sb;
  }

  @Override
  public String getOrderByClause(boolean asc) {
    return adaptee.getOrderByClause(asc);
  }

  @Override
  public String getStatement() {
    return adaptee.getStatement();
  }

  @Override
  public Long getStartTime() {
    return adaptee.getStartTime();
  }

  @Override
  public Long getEndTime() {
    return adaptee.getEndTime();
  }

  @Override
  public Integer getLimit() {
    return adaptee.getLimit();
  }

  @Override
  public Integer getFetchSize() {
    return adaptee.getFetchSize();
  }

  @Override
  public void setFetchSize(Integer fetchSize) {
    adaptee.setFetchSize(fetchSize);
  }

  @Override
  public void addOrderByColumn(String column) {
    adaptee.addOrderByColumn(column);
  }

  @Override
  public void setNoLimit() {
    adaptee.setNoLimit();
  }

  @Override
  public boolean doUpdate() {
    return false;
  }

  public List<String> getOriginalMetricNames() {
    return adaptee.getMetricNames();
  }

  public void setCurrentUuid(byte[] uuid) {
    this.currentUuid = uuid;
  }

  public byte[] getCurrentUuid() {
    return currentUuid;
  }

 @Override
  public void setMetricNamesNotCondition(boolean metricNamesNotCondition) {
    this.metricNamesNotCondition = metricNamesNotCondition;
  }

  @Override
  public void setHostnamesNotCondition(boolean hostNamesNotCondition) {

  }

  @Override
  public void setUuidNotCondition(boolean uuidNotCondition) {

  }
}
