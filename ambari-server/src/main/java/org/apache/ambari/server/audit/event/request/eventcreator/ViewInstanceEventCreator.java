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
import org.apache.ambari.server.audit.event.request.event.AddViewInstanceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.ChangeViewInstanceRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.event.DeleteViewInstanceRequestAuditEvent;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles view instance requests
 * For resource type {@link Resource.Type#ViewInstance}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class ViewInstanceEventCreator implements RequestAuditEventCreator {

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
    return Collections.singleton(Resource.Type.ViewInstance);
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
        return AddViewInstanceRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withType(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "view_name")))
          .withVersion(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "version")))
          .withName(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "instance_name")))
          .withDisplayName(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "label")))
          .withDescription(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "description")))
          .build();

      case PUT:
        return ChangeViewInstanceRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withType(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "view_name")))
          .withVersion(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "version")))
          .withName(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "instance_name")))
          .withDisplayName(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "label")))
          .withDescription(getProperty(request, PropertyHelper.getPropertyId("ViewInstanceInfo", "description")))
          .build();

      case DELETE:
        return DeleteViewInstanceRequestAuditEvent.builder()
          .withTimestamp(DateTime.now())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withUserName(username)
          .withType(request.getResource().getKeyValueMap().get(Resource.Type.View))
          .withVersion(request.getResource().getKeyValueMap().get(Resource.Type.ViewVersion))
          .withName(request.getResource().getKeyValueMap().get(Resource.Type.ViewInstance))
          .build();

      default:
        return null;
    }
  }

  private String getProperty(Request request, String properyId) {
    if (!request.getBody().getPropertySets().isEmpty()) {
      return String.valueOf(request.getBody().getPropertySets().iterator().next().get(properyId));
    }
    return null;
  }

}
