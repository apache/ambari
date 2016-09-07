/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.query.model;

import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_FROM;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_STACK_BY;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_TO;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_UNIT;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_X_AXIS;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_Y_AXIS;

public class AnyGraphSearchCriteria extends CommonSearchCriteria {

  public String getxAxis() {
    return getParam(PARAM_X_AXIS, String.class);
  }

  public void setxAxis(String xAxis) {
    addParam(PARAM_X_AXIS, xAxis);
  }

  public String getyAxis() {
    return getParam(PARAM_Y_AXIS, String.class);
  }

  public void setyAxis(String yAxis) {
    addParam(PARAM_Y_AXIS, yAxis);
  }

  public String getStackBy() {
    return getParam(PARAM_STACK_BY, String.class);
  }

  public void setStackBy(String stackBy) {
    addParam(PARAM_STACK_BY, stackBy);
  }

  public String getUnit() {
    return getParam(PARAM_UNIT, String.class);
  }

  public void setUnit(String unit) {
    addParam(PARAM_UNIT, unit);
  }

  public String getFrom() {
    return getParam(PARAM_FROM, String.class);
  }

  public void setFrom(String from) {
    addParam(PARAM_FROM, from);
  }

  public String getTo() {
    return getParam(PARAM_TO, String.class);
  }

  public void setTo(String to) {
    addParam(PARAM_TO, to);
  }
}
