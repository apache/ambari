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
import org.apache.ambari.logsearch.model.request.DateRangeParamDefinition;

import javax.ws.rs.QueryParam;

public class BaseAuditLogRequest extends BaseLogRequest implements DateRangeParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_FROM)
  private String from;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_TO)
  private String to;

  @Override
  public String getFrom() {
    return from;
  }

  @Override
  public void setFrom(String from) {
    this.from = from;
  }

  @Override
  public String getTo() {
    return to;
  }

  @Override
  public void setTo(String to) {
    this.to = to;
  }
}
