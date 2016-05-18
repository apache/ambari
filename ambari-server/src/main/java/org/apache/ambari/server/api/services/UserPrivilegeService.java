/**
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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
/**
 *  Service responsible for user privilege resource requests.
 */
public class UserPrivilegeService extends PrivilegeService {

  private final String userName;

  public UserPrivilegeService(String userName) {
    this.userName = userName;
  }

  // ----- PrivilegeService --------------------------------------------------

  @Override
  public Response createPrivilege(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response updatePrivilege(String body, HttpHeaders headers, UriInfo ui,
      String privilegeId) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response updatePrivileges(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response deletePrivilege(HttpHeaders headers, UriInfo ui,
      String privilegeId) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  public Response deletePrivileges(String body, HttpHeaders headers, UriInfo ui) {
    return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
  }

  @Override
  protected ResourceInstance createPrivilegeResource(String privilegeId) {
    final Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.User, StringUtils.lowerCase(userName));
    mapIds.put(Resource.Type.UserPrivilege, privilegeId);
    return createResource(Resource.Type.UserPrivilege, mapIds);
  }
}