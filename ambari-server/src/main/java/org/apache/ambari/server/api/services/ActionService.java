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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

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
import java.util.Collections;


/**
 * Service responsible for action definition resource requests.
 */
@Path("/actions/")
public class ActionService extends BaseService {

  /**
   * Handles: GET /actions/{actionName}
   * Get a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName action name
   * @return action definition instance representation
   */
  @GET
  @Path("{actionName}")
  @Produces("text/plain")
  public Response getActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: GET  /actions
   * Get all action definitions.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return action definition collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getActionDefinitions(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createActionDefinitionResource(null));
  }

  /**
   * Handles: POST /actions/{actionName}
   * Create a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the action definition being created
   */
   @POST
   @Path("{actionName}")
   @Produces("text/plain")
   public Response createActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: PUT /actions/{actionName}
   * Update a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the updated action
   */
  @PUT
  @Path("{actionName}")
  @Produces("text/plain")
  public Response updateActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: DELETE /actions/{actionName}
   * Delete a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the deleted action definition
   */
  @DELETE
  @Path("{actionName}")
  @Produces("text/plain")
  public Response deleteActionDefinition(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("actionName") String actionName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createActionDefinitionResource(actionName));
  }

  /**
   * Create a action definition resource instance.
   *
   * @param actionName action name
   *
   * @return a action definition resource instance
   */
  ResourceInstance createActionDefinitionResource(String actionName) {
    return createResource(Resource.Type.Action,
        Collections.singletonMap(Resource.Type.Action, actionName));
  }
}
