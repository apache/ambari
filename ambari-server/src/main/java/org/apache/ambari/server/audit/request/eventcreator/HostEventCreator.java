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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.StartOperationFailedAuditEvent;
import org.apache.ambari.server.audit.StartOperationSucceededAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.audit.request.event.AddHostRequestAuditEvent;
import org.apache.ambari.server.audit.request.event.DeleteAlertGroupRequestAuditEvent;
import org.apache.ambari.server.audit.request.event.DeleteHostRequestAuditEvent;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles host requests (add, delete)
 * For resource type {@link Resource.Type#HostComponent}
 * and request types {@link Request.Type#POST} and {@link Request.Type#DELETE}
 */
public class HostEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = new HashSet<Request.Type>();

  {
    requestTypes.add(Request.Type.POST);
    requestTypes.add(Request.Type.DELETE);
  }

  private Set<Resource.Type> resourceTypes = new HashSet<Resource.Type>();
  {
    resourceTypes.add(Resource.Type.Host);
  }

  /** {@inheritDoc} */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /** {@inheritDoc} */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /** {@inheritDoc} */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    // null makes this default
    return null;
  }

  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    switch(request.getRequestType()) {
      case DELETE:
        return DeleteHostRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withHostName(request.getResource().getKeyValueMap().get(Resource.Type.Host))
          .build();
      case POST:
        return AddHostRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withHostName(getHostName(request))
          .build();
      default:
        return null;
    }
  }

  private String getHostName(Request request) {
    if(!request.getBody().getNamedPropertySets().isEmpty()) {
      return String.valueOf(request.getBody().getNamedPropertySets().iterator().next().getProperties().get(PropertyHelper.getPropertyId("Hosts","host_name")));
    }
    return null;
  }
}
