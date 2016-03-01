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

package org.apache.ambari.server.audit.event.request.eventcreator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.RequestAuditEventCreator;
import org.apache.ambari.server.audit.event.request.event.ActivateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.AdminUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.CreateUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.DeleteUserRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.UserPasswordChangeRequestAuditEvent;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles user requests
 * For resource type {@link Resource.Type#User}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class UserEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = new HashSet<Request.Type>();

  {
    requestTypes.add(Request.Type.POST);
    requestTypes.add(Request.Type.PUT);
    requestTypes.add(Request.Type.DELETE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return Collections.singleton(Resource.Type.User);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    switch (request.getRequestType()) {
      case POST:
        return CreateUserRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withCreatedUsername(getUsername(request))
          .withActive(isActive(request))
          .withAdmin(isAdmin(request))
          .build();
      case DELETE:
        return DeleteUserRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withDeletedUsername(request.getResource().getKeyValueMap().get(Resource.Type.User))
          .build();
      case PUT:
        if (hasActive(request)) {
          return ActivateUserRequestAuditEvent.builder()
            .withTimestamp(DateTime.now())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
            .withUserName(username)
            .withAffectedUsername(getUsername(request))
            .withActive(isActive(request))
            .build();
        }
        if (hasAdmin(request)) {
          return AdminUserRequestAuditEvent.builder()
            .withTimestamp(DateTime.now())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
            .withUserName(username)
            .withAffectedUsername(getUsername(request))
            .withAdmin(isAdmin(request))
            .build();
        }
        if (hasOldPassword(request)) {
          return UserPasswordChangeRequestAuditEvent.builder()
            .withTimestamp(DateTime.now())
            .withRequestType(request.getRequestType())
            .withResultStatus(result.getStatus())
            .withUrl(request.getURI())
            .withRemoteIp(request.getRemoteAddress())
            .withUserName(username)
            .withAffectedUsername(getUsername(request))
            .build();
        }
      default:
        break;
    }
    return null;
  }


  private boolean isAdmin(Request request) {
    return hasAdmin(request) && "true".equals(request.getBody().getPropertySets().iterator().next().get(PropertyHelper.getPropertyId("Users", "admin")));
  }

  private boolean isActive(Request request) {
    return hasActive(request) && "true".equals(request.getBody().getPropertySets().iterator().next().get(PropertyHelper.getPropertyId("Users", "active")));
  }

  private boolean hasAdmin(Request request) {
    return !request.getBody().getPropertySets().isEmpty() && request.getBody().getPropertySets().iterator().next().containsKey(PropertyHelper.getPropertyId("Users", "admin"));
  }

  private boolean hasActive(Request request) {
    return !request.getBody().getPropertySets().isEmpty() && request.getBody().getPropertySets().iterator().next().containsKey(PropertyHelper.getPropertyId("Users", "active"));
  }

  private boolean hasOldPassword(Request request) {
    return !request.getBody().getPropertySets().isEmpty() && request.getBody().getPropertySets().iterator().next().containsKey(PropertyHelper.getPropertyId("Users", "old_password"));
  }

  private String getUsername(Request request) {
    if (!request.getBody().getPropertySets().isEmpty()) {
      return String.valueOf(request.getBody().getPropertySets().iterator().next().get(PropertyHelper.getPropertyId("Users", "user_name")));
    }
    return null;
  }

}
