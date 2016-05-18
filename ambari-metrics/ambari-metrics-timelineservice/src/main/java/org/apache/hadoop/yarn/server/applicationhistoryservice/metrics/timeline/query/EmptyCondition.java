/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;

import org.apache.hadoop.metrics2.sink.timeline.Precision;

import java.util.List;

/**
 * Encapsulate a Condition with pre-formatted and pre-parsed query string.
 */
public class EmptyCondition implements Condition {
  String statement;
  boolean doUpdate = false;

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public List<String> getMetricNames() {
    return null;
  }

  @Override
  public boolean isPointInTime() {
    return false;
  }

  @Override
  public boolean isGrouped() {
    return true;
  }

  @Override
  public void setStatement(String statement) {
    this.statement = statement;
  }

  @Override
  public List<String> getHostnames() {
    return null;
  }

  @Override
  public Precision getPrecision() {
    return null;
  }

  @Override
  public void setPrecision(Precision precision) {

  }

  @Override
  public String getAppId() {
    return null;
  }

  @Override
  public String getInstanceId() {
    return null;
  }

  @Override
  public StringBuilder getConditionClause() {
    return null;
  }

  @Override
  public String getOrderByClause(boolean asc) {
    return null;
  }

  @Override
  public String getStatement() {
    return statement;
  }

  @Override
  public Long getStartTime() {
    return null;
  }

  @Override
  public Long getEndTime() {
    return null;
  }

  @Override
  public Integer getLimit() {
    return null;
  }

  @Override
  public Integer getFetchSize() {
    return null;
  }

  @Override
  public void setFetchSize(Integer fetchSize) {

  }

  @Override
  public void addOrderByColumn(String column) {

  }

  @Override
  public void setNoLimit() {

  }

  public void setDoUpdate(boolean doUpdate) {
    this.doUpdate = doUpdate;
  }

  @Override
  public boolean doUpdate() {
    return doUpdate;
  }

  @Override
  public String toString() {
    return "EmptyCondition{ " +
      " statement = " + this.getStatement() +
      " doUpdate = " + this.doUpdate() +
      " }";
  }
}
