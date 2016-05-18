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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * UserAuthorizationService is a read-only service responsible for user authorization resource requests.
 * <p/>
 * The result sets returned by this service represent the set of authorizations assigned to a given user.
 * Authorizations are tied to a resource, so a user may have the multiple authorization entries for the
 * same authorization id (for example VIEW.USE), however each will represnet a different view instance.
 */
public class UserAuthorizationService extends BaseService {

  /**
   * The username this UserAuthorizationService is linked to
   */
  private final String username;

  /**
   * Create a new UserAuthorizationService that is linked to a particular user
   *
   * @param username the username of the user to link thi UserAuthorizationService to
   */
  public UserAuthorizationService(String username) {
    this.username = username;
  }

  /**
   * Handles: GET  /users/{user_name}/authorizations
   * Get all authorizations for the relative user.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return authorizations collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getAuthorizations(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(null));
  }

  /**
   * Handles: GET  /permissions/{user_name}/authorizations/{authorization_id}
   * Get a specific authorization.
   *
   * @param headers         http headers
   * @param ui              uri info
   * @param authorizationId authorization ID
   * @return authorization instance representation
   */
  @GET
  @Path("{authorization_id}")
  @Produces("text/plain")
  public Response getAuthorization(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("authorization_id") String authorizationId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(authorizationId));
  }

  /**
   * Create an authorization resource.
   *
   * @param authorizationId authorization id
   * @return an authorization resource instance
   */
  protected ResourceInstance createAuthorizationResource(String authorizationId) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.User, StringUtils.lowerCase(username));
    mapIds.put(Resource.Type.UserAuthorization, authorizationId);
    return createResource(Resource.Type.UserAuthorization, mapIds);
  }
}
