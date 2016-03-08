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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddComponentToHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.AddHostRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteHostRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles host requests (add, delete, add component)
 * For resource type {@link Resource.Type#HostComponent}
 * and request types {@link Request.Type#POST}, {@link Request.Type#DELETE} and {@link Request.Type#QUERY_POST}
 */
public class HostEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.QUERY_POST, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Host).build();

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
    // null makes this default
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {
    String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

    switch (request.getRequestType()) {
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
      case QUERY_POST:
        return AddComponentToHostRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withHostName(getHostNameFromQuery(request))
          .withComponent(getHostComponent(request))
          .build();
      default:
        return null;
    }
  }

  /**
   * Returns hostname from the request
   * @param request
   * @return
   */
  private String getHostName(Request request) {
    if (!request.getBody().getNamedPropertySets().isEmpty()) {
      return String.valueOf(request.getBody().getNamedPropertySets().iterator().next().getProperties().get(PropertyHelper.getPropertyId("Hosts", "host_name")));
    }
    return null;
  }

  /**
   * Returns component name from the request
   * @param request
   * @return
   */
  private String getHostComponent(Request request) {
    if (!request.getBody().getNamedPropertySets().isEmpty()) {
      Set<Map<String, String>> set = (Set<Map<String, String>>) request.getBody().getNamedPropertySets().iterator().next().getProperties().get("host_components");
      if (set != null && !set.isEmpty()) {
        return set.iterator().next().get(PropertyHelper.getPropertyId("HostRoles", "component_name"));
      }
    }
    return null;
  }

  /**
   * Returns hostname from the query string of the request
   * @param request
   * @return
   */
  private String getHostNameFromQuery(Request request) {
    final String key = PropertyHelper.getPropertyId("Hosts", "host_name");
    if (request.getBody().getQueryString().contains(key)) {
      String q = request.getBody().getQueryString();
      int startIndex = q.indexOf(key) + key.length() + 1;
      int endIndex = q.indexOf("&", startIndex) == -1 ? q.length() : q.indexOf("&", startIndex);
      return q.substring(startIndex, endIndex);
    }
    return null;
  }
}
