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
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link Resource.Type#Service}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class ServiceEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = new HashSet<Request.Type>();

  {
    requestTypes.add(Request.Type.POST);
    requestTypes.add(Request.Type.PUT);
    requestTypes.add(Request.Type.DELETE);
  }

  private Set<Resource.Type> resourceTypes = new HashSet<Resource.Type>();
  {
    resourceTypes.add(Resource.Type.Service);
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

    String operation = getOperation(request);

    if (result.getStatus().isErrorState()) {
      return StartOperationFailedAuditEvent.builder()
        .withOperation(operation)
        .withUserName(username)
        .withRemoteIp(request.getRemoteAddress())
        .withTimestamp(DateTime.now())
        .withReason(result.getStatus().getMessage())
        .build();
    } else {
      Long requestId = null;
      if (containsRequestId(result)) {
        requestId = getRequestId(result);
      }
      return StartOperationSucceededAuditEvent.builder()
        .withOperation(operation)
        .withUserName(username)
        .withRemoteIp(request.getRemoteAddress())
        .withTimestamp(DateTime.now())
        .withRequestId(String.valueOf(requestId))
        .build();
    }
  }

  private String getOperation(Request request) {
    if (request.getBody().getRequestInfoProperties().containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      String operation = "";
      if("CLUSTER".equals(request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_LEVEL_ID))) {
        for (Map<String, Object> map : request.getBody().getPropertySets()) {
          if (map.containsKey(PropertyHelper.getPropertyId("ServiceInfo", "state"))) {
            operation = String.valueOf(map.get(PropertyHelper.getPropertyId("ServiceInfo", "state"))) + ": all services"
              + " (" + request.getBody().getRequestInfoProperties().get(RequestOperationLevel.OPERATION_CLUSTER_ID) + ")";
            break;
          }
        }
      }
      return operation;
    }

    for (Map<String, Object> map : request.getBody().getPropertySets()) {
      if (map.containsKey(PropertyHelper.getPropertyId("ServiceInfo", "maintenance_state"))) {
        return "Turn " + map.get(PropertyHelper.getPropertyId("ServiceInfo", "maintenance_state")) + " Maintenance Mode for " + map.get(PropertyHelper.getPropertyId("ServiceInfo", "service_name"));
      }
    }
    return null;
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
