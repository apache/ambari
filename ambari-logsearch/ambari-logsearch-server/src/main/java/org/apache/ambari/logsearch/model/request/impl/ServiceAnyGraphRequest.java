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
package org.apache.ambari.logsearch.model.request.impl;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.AnyGraphParamDefinition;
import org.apache.ambari.logsearch.model.request.UnitParamDefinition;

import javax.ws.rs.QueryParam;

public class ServiceAnyGraphRequest extends ServiceLogRequest
  implements AnyGraphParamDefinition, UnitParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_XAXIS)
  private String xAxis;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_YAXIS)
  private String yAxis;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_STACK_BY)
  private String stackBy;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_UNIT)
  private String unit;

  @Override
  public String getxAxis() {
    return xAxis;
  }

  @Override
  public void setxAxis(String xAxis) {
    this.xAxis = xAxis;
  }

  @Override
  public String getyAxis() {
    return yAxis;
  }

  @Override
  public void setyAxis(String yAxis) {
    this.yAxis = yAxis;
  }

  @Override
  public String getStackBy() {
    return stackBy;
  }

  @Override
  public void setStackBy(String stackBy) {
    this.stackBy = stackBy;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public void setUnit(String unit) {
    this.unit = unit;
  }
}
