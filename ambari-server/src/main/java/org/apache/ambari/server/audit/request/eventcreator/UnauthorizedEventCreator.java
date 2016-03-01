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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.AccessUnauthorizedAuditEvent;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public class UnauthorizedEventCreator implements RequestAuditEventCreator{

  @Override
  public Set<Request.Type> getRequestTypes() {
    return null;
  }

  @Override
  public Set<Resource.Type> getResourceTypes() {
    return null;
  }

  private Set<ResultStatus.STATUS> statuses = new HashSet<>();

  {
    statuses.add(ResultStatus.STATUS.UNAUTHORIZED);
    statuses.add(ResultStatus.STATUS.FORBIDDEN);
  }

  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return statuses;
  }

  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
    AccessUnauthorizedAuditEvent ae = AccessUnauthorizedAuditEvent.builder()
      .withRemoteIp(request.getRemoteAddress())
      .withResourcePath(request.getURI())
      .withTimestamp(DateTime.now())
      .withUserName(username)
      .build();

    return ae;
  }
}
