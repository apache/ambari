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
package org.apache.ambari.server.api.services.users;

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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for user requests.
 */
@Path("/users/")
@Api(value = "Users", description = "Endpoint for user specific operations")
public class UserService extends BaseService {

  /**
   * Gets all users.
   * Handles: GET /users requests.
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all users", nickname = "UserService#getUsers", notes = "Returns details of all users.", response = UserResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user details", defaultValue = "Users/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort users (asc | desc)", defaultValue = "Users/user_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = UserResponse.class, responseContainer = "List")}
  )
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
  @ApiOperation(value = "Get single user", nickname = "UserService#getUser", notes = "Returns user details.", response = UserResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user details", defaultValue = "Users", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = UserResponse.class)}
  )
  public Response getUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @ApiParam(value = "user name", required = true, defaultValue = "admin") @PathParam("userName") String userName) {
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
  @POST @ApiIgnore // until documented
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
  @ApiOperation(value = "Create new user", nickname = "UserService#createUser", notes = "Creates user resource.")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.UserRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response createUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
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
  @ApiOperation(value = "Update user detail", nickname = "UserService#updateUser", notes = "Updates user resource.")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.UserRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response updateUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createUserResource(userName));
  }

  /**
   * Deletes a user.
   * Handles:  DELETE /users/{userName}
   */
  @DELETE
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Delete single user", nickname = "UserService#deleteUser", notes = "Delete user resource.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response deleteUser(@Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createUserResource(userName));
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
