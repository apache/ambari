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
import org.apache.ambari.logsearch.model.request.LogTruncatedParamDefinition;

import javax.ws.rs.QueryParam;

public class ServiceLogTruncatedRequest extends ServiceLogRequest implements LogTruncatedParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_ID)
  private String id;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_SCROLL_TYPE)
  private String scrollType;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_NUMBER_ROWS)
  private Integer numberRows;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getScrollType() {
    return scrollType;
  }

  @Override
  public void setScrollType(String scrollType) {
    this.scrollType = scrollType;
  }

  @Override
  public Integer getNumberRows() {
    return numberRows;
  }

  @Override
  public void setNumberRows(Integer numberRows) {
    this.numberRows = numberRows;
  }
}
