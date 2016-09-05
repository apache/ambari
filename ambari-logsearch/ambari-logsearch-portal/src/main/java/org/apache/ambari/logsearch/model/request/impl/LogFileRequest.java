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
import org.apache.ambari.logsearch.model.request.LogFileParamDefinition;
import org.apache.ambari.logsearch.model.request.SearchRequest;

import javax.ws.rs.QueryParam;

public class LogFileRequest implements SearchRequest, LogFileParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_COMPONENT)
  private String component;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_HOST)
  private String host;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_LOG_TYPE)
  private String logType;

  @Override
  public String getComponent() {
    return component;
  }

  @Override
  public void setComponent(String component) {
    this.component = component;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String getLogType() {
    return logType;
  }

  @Override
  public void setLogType(String logType) {
    this.logType = logType;
  }
}
