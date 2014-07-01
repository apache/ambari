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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;

/**
 * Service responsible for user groups requests.
 */
@Path("/groups/")
public class GroupService extends BaseService {
  /**
   * Gets all groups.
   * Handles: GET /groups requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   */
  @GET
  @Produces("text/plain")
  public Response getGroups(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createGroupResource(null));
  }

  /**
   * Gets a single group.
   * Handles: GET /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the specified group
   */
  @GET
  @Path("{groupName}")
  @Produces("text/plain")
  public Response getGroup(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createGroupResource(groupName));
  }

  /**
   * Creates a group.
   * Handles: POST /groups requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @return information regarding the created group
   */
   @POST
   @Produces("text/plain")
   public Response createGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createGroupResource(null));
  }

  /**
   * Creates a group.
   * Handles: POST /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the created group
   *
   * @deprecated Use requests to /groups instead.
   */
   @POST
   @Deprecated
   @Path("{groupName}")
   @Produces("text/plain")
   public Response createGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("groupName") String groupName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createGroupResource(groupName));
  }

  /**
   * Deletes a group.
   * Handles:  DELETE /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the deleted group
   */
  @DELETE
  @Path("{groupName}")
  @Produces("text/plain")
  public Response deleteGroup(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createGroupResource(groupName));
  }

  /**
   * Get the members sub-resource.
   *
   * @param groupName    the group name
   * @return the members service
   */
  @Path("{groupName}/members")
  public MemberService getMemberHandler(@PathParam("groupName") String groupName) {
    return new MemberService(groupName);
  }

  /**
   * Create a group resource instance.
   *
   * @param groupName group name
   *
   * @return a group resource instance
   */
  private ResourceInstance createGroupResource(String groupName) {
    return createResource(Resource.Type.Group,
        Collections.singletonMap(Resource.Type.Group, groupName));
  }
}
