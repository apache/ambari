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
 * Service responsible for handling REST requests for the /blueprints endpoint.
 * This blueprint resource is a blueprint template meaning that it doesn't contain
 * any cluster specific information.  Updates are not permitted as blueprints are
 * immutable.
 */
@Path("/blueprints/")
public class BlueprintService extends BaseService {

  /**
   * Handles: GET  /blueprints
   * Get all blueprints.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @return blueprint collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getBlueprints(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createBlueprintResource(null));
  }

  /**
   * Handles: GET /blueprints/{blueprintID}
   * Get a specific blueprint.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param blueprintName  blueprint id
   * @return blueprint instance representation
   */
  @GET
  @Path("{blueprintName}")
  @Produces("text/plain")
  public Response getBlueprint(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("blueprintName") String blueprintName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: POST /blueprints/{blueprintID}
   * Create a specific blueprint.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param blueprintName blueprint id
   * @return information regarding the created blueprint
   */
  @POST
  @Path("{blueprintName}")
  @Produces("text/plain")
  public Response createBlueprint(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                  @PathParam("blueprintName") String blueprintName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: DELETE /blueprints/{blueprintID}
   * Delete a specific blueprint.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param blueprintName blueprint name
   * @return information regarding the deleted blueprint
   */
  @DELETE
  @Path("{blueprintName}")
  @Produces("text/plain")
  public Response deleteBlueprint(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("blueprintName") String blueprintName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: DELETE /blueprints
   * Delete a set of blueprints that match a predicate.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @return information regarding the deleted blueprint
   */
  @DELETE
  @Produces("text/plain")
  public Response deleteBlueprints(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createBlueprintResource(null));
  }

  /**
   * Create a blueprint resource instance.
   *
   * @param blueprintName blueprint name
   *
   * @return a blueprint resource instance
   */
  ResourceInstance createBlueprintResource(String blueprintName) {
    return createResource(Resource.Type.Blueprint,
        Collections.singletonMap(Resource.Type.Blueprint, blueprintName));
  }
}
