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
import org.apache.ambari.logsearch.model.request.UserConfigParamDefinition;

import javax.ws.rs.QueryParam;

public class UserConfigRequest extends CommonSearchRequest implements UserConfigParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_USER_ID)
  private String userId;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_FILE_NAME)
  private String filterName;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_ROW_TYPE)
  private String rowType;

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String getFilterName() {
    return filterName;
  }

  @Override
  public void setFilterName(String filterName) {
    this.filterName = filterName;
  }

  @Override
  public String getRowType() {
    return rowType;
  }

  @Override
  public void setRowType(String rowType) {
    this.rowType = rowType;
  }
}
