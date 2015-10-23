package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;

import org.apache.hadoop.metrics2.sink.timeline.Precision;

import java.util.List;

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
public interface Condition {
  boolean isEmpty();

  List<String> getMetricNames();
  boolean isPointInTime();
  boolean isGrouped();
  void setStatement(String statement);
  List<String> getHostnames();
  Precision getPrecision();
  void setPrecision(Precision precision);
  String getAppId();
  String getInstanceId();
  StringBuilder getConditionClause();
  String getOrderByClause(boolean asc);
  String getStatement();
  Long getStartTime();
  Long getEndTime();
  Integer getLimit();
  Integer getFetchSize();
  void setFetchSize(Integer fetchSize);
  void addOrderByColumn(String column);
  void setNoLimit();
  boolean doUpdate();
}
