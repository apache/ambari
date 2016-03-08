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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.ClusterPrivilegeChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.PrivilegeChangeRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles privilege requests
 * For resource type {@link Resource.Type#ClusterPrivilege}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT}
 */
public class PrivilegeEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.ClusterPrivilege).build();

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
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    Map<String, List<String>> users = getEntities(request, "USER");
    Map<String, List<String>> groups = getEntities(request, "GROUP");

    switch (request.getRequestType()) {
      case PUT:
        return ClusterPrivilegeChangeRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withUsers(users)
          .withGroups(groups)
          .build();
      case POST:
        String role = users.isEmpty() ? (groups.isEmpty() ? null : groups.keySet().iterator().next()) : users.keySet().iterator().next();
        return PrivilegeChangeRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withRole(role)
          .withGroup(groups.get(role) == null ? null : groups.get(role).get(0))
          .withUser(users.get(role) == null ? null : users.get(role).get(0))
          .withOperation((users.isEmpty() ? (groups.isEmpty() ? "" : "Group ") : "User ") + "role change")
          .build();
      default:
        return null;
    }
  }

  /**
   * Assembles entities from the request. The result can contain users or groups based on the value of type parameter
   * @param request
   * @param type
   * @return a map of role -> [user|group] names
   */
  private Map<String, List<String>> getEntities(final Request request, final String type) {
    Map<String, List<String>> entities = new HashMap<String, List<String>>();

    for (Map<String, Object> propertyMap : request.getBody().getPropertySets()) {
      String ptype = String.valueOf(propertyMap.get(PropertyHelper.getPropertyId("PrivilegeInfo", "principal_type")));
      if (type.equals(ptype)) {
        String role = String.valueOf(propertyMap.get(PropertyHelper.getPropertyId("PrivilegeInfo", "permission_name")));
        String name = String.valueOf(propertyMap.get(PropertyHelper.getPropertyId("PrivilegeInfo", "principal_name")));
        if (!entities.containsKey(role)) {
          entities.put(role, new LinkedList<String>());
        }

        entities.get(role).add(name);
      }
    }
    return entities;
  }

}
