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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddUserToGroupRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.MembershipChangeRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.RemoveUserFromGroupRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles member requests
 * For resource type {@link Resource.Type#Member}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class MemberEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Member).build();

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

    switch (request.getRequestType()) {
      case POST:
        return AddUserToGroupRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withAffectedUserName(getUserName(request))
          .withGroupName(getGroupName(request))
          .build();
      case DELETE:
        return RemoveUserFromGroupRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withAffectedUserName(getUserName(request))
          .withGroupName(getGroupName(request))
          .build();
      case PUT:
        return MembershipChangeRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withGroupName(getGroupNameForPut(request))
          .withUserNameList(getUsers(request))
          .build();
      default:
        return null;
    }
  }

  /**
   * Returns users from the request
   * @param request
   * @return
   */
  private List<String> getUsers(Request request) {
    List<String> users = new LinkedList<String>();

    for (Map<String, Object> propertyMap : request.getBody().getPropertySets()) {
      String userName = String.valueOf(propertyMap.get(PropertyHelper.getPropertyId("MemberInfo", "user_name")));
      users.add(userName);
    }
    return users;
  }

  /**
   * Returns target group name from the request. This is called when PUT request type is used.
   * @param request
   * @return
   */
  private String getGroupNameForPut(Request request) {

    for (Map<String, Object> propertyMap : request.getBody().getPropertySets()) {
      return String.valueOf(propertyMap.get(PropertyHelper.getPropertyId("MemberInfo", "group_name")));
    }
    return null;
  }

  /**
   * Returns username from the request
   * @param request
   * @return
   */
  private String getUserName(Request request) {
    return request.getResource().getKeyValueMap().get(Resource.Type.Member);
  }

  /**
   * Returns groupname from the request
   * @param request
   * @return
   */
  private String getGroupName(Request request) {
    return request.getResource().getKeyValueMap().get(Resource.Type.Group);
  }


}
