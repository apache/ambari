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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.services;

import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service responsible for user requests.
 */
@Path("/users/")
public class UserService extends BaseService {

  /**
   * Gets all users.
   * Handles: GET /users requests.
   */
  @GET
  @Produces("text/plain")
  public Response getUsers(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createUserResource(null));
  }

  /**
   * Gets a single user.
   * Handles: GET /users/{username} requests
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param userName    the username
   * @return information regarding the created user
   */
  @GET
  @Path("{userName}")
  @Produces("text/plain")
  public Response getUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.GET, createUserResource(userName));
  }

  /**
   * Creates a user.
   * Handles: POST /users
   *
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created user
   */
  @POST
  @Produces("text/plain")
  public Response createUser(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createUserResource(null));
  }

  /**
   * Creates a user.
   * Handles: POST /users/{username}
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param userName    the username
   * @return information regarding the created user
   */
  @POST
  @Path("{userName}")
  @Produces("text/plain")
  public Response createUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createUserResource(userName));
  }

  /**
   * Updates a specific user.
   * Handles: PUT /users/{userName}
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param userName   the username
   * @return information regarding the updated user
   */
  @PUT
  @Path("{userName}")
  @Produces("text/plain")
  public Response updateUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("userName") String userName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createUserResource(userName));
  }

  /**
   * Deletes a user.
   * Handles:  DELETE /users/{userName}
   */
  @DELETE
  @Path("{userName}")
  @Produces("text/plain")
  public Response deleteUser(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createUserResource(userName));
  }

  /**
   * Gets the user privilege service
   */
  @Path("{userName}/privileges")
  public PrivilegeService getPrivilegeService(@Context javax.ws.rs.core.Request request,
                                              @PathParam ("userName") String userName) {

    return new UserPrivilegeService(userName);
  }

  /**
   * Gets the active widget layout service
   */
  @Path("{userName}/activeWidgetLayouts")
  public ActiveWidgetLayoutService getWidgetLayoutService(@Context javax.ws.rs.core.Request request,
                                                    @PathParam ("userName") String userName) {

    return new ActiveWidgetLayoutService(userName);
  }

  /**
   * Gets the user authorization service.
   *
   * @param request  the request
   * @param username the username
   * @return the UserAuthorizationService
   */
  @Path("{userName}/authorizations")
  public UserAuthorizationService getUserAuthorizations(
      @Context javax.ws.rs.core.Request request, @PathParam("userName") String username) {
    return new UserAuthorizationService(username);
  }

  /**
   * Create a user resource instance.
   *
   * @param userName  user name
   *
   * @return a user resource instance
   */
  private ResourceInstance createUserResource(String userName) {
    return createResource(Resource.Type.User,
        Collections.singletonMap(Resource.Type.User, userName));
  }
}
