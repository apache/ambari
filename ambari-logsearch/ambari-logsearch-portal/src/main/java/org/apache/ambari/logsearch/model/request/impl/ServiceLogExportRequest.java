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
import org.apache.ambari.logsearch.model.request.FormatParamDefinition;
import org.apache.ambari.logsearch.model.request.UtcOffsetParamDefinition;

import javax.ws.rs.QueryParam;

public class ServiceLogExportRequest extends ServiceLogRequest implements FormatParamDefinition, UtcOffsetParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_FORMAT)
  private String format;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_UTC_OFFSET)
  private String utcOffset;

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public String getUtcOffset() {
    return utcOffset;
  }

  @Override
  public void setUtcOffset(String utcOffset) {
    this.utcOffset = utcOffset;
  }
}
