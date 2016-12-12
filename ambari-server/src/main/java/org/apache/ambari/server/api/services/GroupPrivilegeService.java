/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 *  Service responsible for group privilege resource requests.
 */
public class GroupPrivilegeService extends PrivilegeService {

  private final String groupName;

  public GroupPrivilegeService(String groupName) {
    this.groupName = groupName;
  }

  // ----- PrivilegeService --------------------------------------------------

  @Override
  public Response createPrivilege(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response updatePrivilege(String body, HttpHeaders headers, UriInfo ui, String privilegeId) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response updatePrivileges(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response deletePrivilege(HttpHeaders headers, UriInfo ui, String privilegeId) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response deletePrivileges(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  protected ResourceInstance createPrivilegeResource(String privilegeId) {
    final Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Group, groupName);
    mapIds.put(Resource.Type.GroupPrivilege, privilegeId);
    return createResource(Resource.Type.GroupPrivilege, mapIds);
  }
}