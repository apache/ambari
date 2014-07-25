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

import java.util.HashMap;
import java.util.Map;

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
 * Service responsible for user membership requests.
 */
public class MemberService extends BaseService {
  /**
   * Name of the group.
   */
  private String groupName;

  /**
   * Constructor.
   *
   * @param groupName name of the group
   */
  public MemberService(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Creates new members.
   * Handles: POST /groups/{groupname}/members requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @return information regarding the created member
   */
   @POST
   @Produces("text/plain")
   public Response createMember(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createMemberResource(groupName, null));
  }

  /**
   * Creates a new member.
   * Handles: POST /groups/{groupname}/members/{username} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param userName     the user name
   * @return information regarding the created member
   */
   @POST
   @Path("{userName}")
   @Produces("text/plain")
   public Response createMember(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createMemberResource(groupName, userName));
  }

   /**
    * Deletes a member.
    * Handles:  DELETE /groups/{groupname}/members/{username} requests.
    *
    * @param headers      http headers
    * @param ui           uri info
    * @param userName     the user name
    * @return information regarding the deleted group
    */
   @DELETE
   @Path("{userName}")
   @Produces("text/plain")
   public Response deleteMember(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("userName") String userName) {
     return handleRequest(headers, null, ui, Request.Type.DELETE, createMemberResource(groupName, userName));
   }

  /**
   * Gets all members.
   * Handles: GET /groups/{groupname}/members requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @return information regarding all members
   */
  @GET
  @Produces("text/plain")
  public Response getMembers(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createMemberResource(groupName, null));
  }

  /**
   * Gets member.
   * Handles: GET /groups/{groupname}/members/{username} requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param userName   the user name
   * @return information regarding the specific member
   */
  @GET
  @Path("{userName}")
  @Produces("text/plain")
  public Response getMember(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createMemberResource(groupName, userName));
  }

  /**
   * Updates all members.
   * Handles: PUT /groups/{groupname}/members requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @return status of the request
   */
  @PUT
  @Produces("text/plain")
  public Response updateMembers(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createMemberResource(groupName, null));
  }

  /**
   * Create a member resource instance.
   *
   * @param groupName  group name
   * @param userName   user name
   *
   * @return a member resource instance
   */
  private ResourceInstance createMemberResource(String groupName, String userName) {
    final Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Group, groupName);
    mapIds.put(Resource.Type.Member, userName);
    return createResource(Resource.Type.Member, mapIds);
  }
}
