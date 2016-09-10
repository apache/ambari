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

import io.swagger.annotations.ApiParam;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.LogParamDefinition;

import javax.ws.rs.QueryParam;

public class BaseLogRequest extends QueryRequest implements LogParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_I_MESSAGE)
  private String iMessage;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_MUST_BE)
  private String mustBe;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_MUST_NOT)
  private String mustNot;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_EXCLUDE_QUERY)
  private String excludeQuery;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_INCLUDE_QUERY)
  private String includeQuery;

  @Override
  public String getiMessage() {
    return iMessage;
  }

  @Override
  public void setiMessage(String iMessage) {
    this.iMessage = iMessage;
  }

  @Override
  public String getMustBe() {
    return mustBe;
  }

  @Override
  public void setMustBe(String mustBe) {
    this.mustBe = mustBe;
  }

  @Override
  public String getMustNot() {
    return mustNot;
  }

  @Override
  public void setMustNot(String mustNot) {
    this.mustNot = mustNot;
  }

  @Override
  public String getIncludeQuery() {
    return includeQuery;
  }

  @Override
  public void setIncludeQuery(String includeQuery) {
    this.includeQuery = includeQuery;
  }

  @Override
  public String getExcludeQuery() {
    return excludeQuery;
  }

  @Override
  public void setExcludeQuery(String excludeQuery) {
    this.excludeQuery = excludeQuery;
  }
}
