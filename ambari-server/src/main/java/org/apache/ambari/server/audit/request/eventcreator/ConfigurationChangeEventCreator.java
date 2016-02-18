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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.request.ConfigurationChangeRequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * This creator handles operation requests (start, stop, install, etc)
 * For resource type {@link Resource.Type#HostComponent}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class ConfigurationChangeEventCreator implements RequestAuditEventCreator {

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
    return Collections.singleton(Resource.Type.Cluster);
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

    return ConfigurationChangeRequestAuditEvent.builder()
      .withTimestamp(DateTime.now())
      .withRequestType(request.getRequestType())
      .withResultStatus(result.getStatus())
      .withUrl(request.getURI())
      .withRemoteIp(request.getRemoteAddress())
      .withUserName(username)
      .withVersionNote(getServiceConfigVersionNote(result))
      .withVersionNumber(getServiceConfigVersion(result))
      .build();
  }

  private String getServiceConfigVersion(Result result) {
    Map<String, Object> map = getServiceConfigMap(result);
    return map == null ? null : String.valueOf(map.get("service_config_version"));
  }

  private String getServiceConfigVersionNote(Result result) {
    Map<String, Object> map = getServiceConfigMap(result);
    return map == null ? null : String.valueOf(map.get("service_config_version_note"));
  }

  private Map<String, Object> getServiceConfigMap(Result result) {
    if (result.getResultTree().getChild("resources") != null &&
      !result.getResultTree().getChild("resources").getChildren().isEmpty() &&
      result.getResultTree().getChild("resources").getChildren().iterator().next().getObject() != null) {
      return result.getResultTree().getChild("resources").getChildren().iterator().next().getObject().getPropertiesMap().get("");
    }
    return null;
  }

}
