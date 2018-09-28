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
package org.apache.ambari.logsearch.model.request.impl.query;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.FieldAuditLogRequest;

import javax.ws.rs.QueryParam;

public class FieldAuditLogQueryRequest extends BaseLogQueryRequest implements FieldAuditLogRequest {
  @QueryParam(LogSearchConstants.REQUEST_PARAM_FIELD)
  private String field;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_USERS)
  private String userList;

  @Override
  public String getField() {
    return field;
  }

  @Override
  public void setField(String field) {
    this.field = field;
  }

  @Override
  public String getUserList() {
    return userList;
  }

  @Override
  public void setUserList(String userList) {
    this.userList = userList;
  }
}
