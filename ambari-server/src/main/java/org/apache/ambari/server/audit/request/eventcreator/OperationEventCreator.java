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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.StartOperationFailedAuditEvent;
import org.apache.ambari.server.audit.StartOperationSucceededAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.utils.RequestUtils;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link org.apache.ambari.server.controller.spi.Resource.Type#HostComponent}
 * and request types {@link org.apache.ambari.server.api.services.Request.Type#POST}, {@link org.apache.ambari.server.api.services.Request.Type#PUT} and {@link org.apache.ambari.server.api.services.Request.Type#DELETE}
 */
public class OperationEventCreator implements RequestAuditEventCreator{

  /**
   * Set of {@link org.apache.ambari.server.api.services.Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = new HashSet<Request.Type>();

  {
    requestTypes.add(Request.Type.POST);
    requestTypes.add(Request.Type.PUT);
    requestTypes.add(Request.Type.DELETE);
  }

  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  @Override
  public Resource.Type getResourceType() {
    return Resource.Type.HostComponent;
  }

  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    if(result.getStatus().isErrorState()) {
      return StartOperationFailedAuditEvent.builder()
        .withRequestDetails(request.getBody().getBody())
        .withUserName(username)
        .withRemoteIp(RequestUtils.getRemoteAddress(request))
        .withTimestamp(new DateTime())
        .withReason(result.getStatus().getMessage())
        .build();
    } else {
      Long requestId = null;
      if(containsRequestId(result)) {
        requestId = getRequestId(result);
      }
      return StartOperationSucceededAuditEvent.builder()
        .withRequestDetails(request.getBody().getBody())
        .withUserName(username)
        .withRemoteIp(RequestUtils.getRemoteAddress(request))
        .withTimestamp(new DateTime())
        .withRequestId(String.valueOf(requestId))
        .build();
    }
  }

  private Long getRequestId(Result result) {
    return (Long) result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests").get("id");
  }

  private boolean containsRequestId(Result result) {
    return result.getResultTree().getChild("request") != null
      && result.getResultTree().getChild("request").getObject() != null
      && result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests") != null
      && result.getResultTree().getChild("request").getObject().getPropertiesMap().get("Requests").get("id") != null;
  }
}
