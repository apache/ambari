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

package org.apache.ambari.server.api.services.views;

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
import org.apache.ambari.server.controller.ViewResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for view resource requests.
 */
@Path("/views/")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewService extends BaseService {

  /**
   * Handles: GET  /views
   * Get all views.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return view collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all views", nickname = "ViewService#getViews", notes = "Returns details of all views.", response = ViewResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view details", defaultValue = "ViewInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort users (asc | desc)", defaultValue = "ViewInfo/view_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewResponse.class, responseContainer = "List")}
  )
  public Response getViews(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createViewResource(null));
  }

  /**
   * Handles: GET /views/{viewID}
   * Get a specific view.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   *
   * @return view instance representation
   */
  @GET
  @Path("{viewName}")
  @Produces("text/plain")
  @ApiOperation(value = "Get single view", nickname = "ViewService#getView", notes = "Returns view details.", response = ViewResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view details", defaultValue = "ViewInfo", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewResponse.class)}
  )
  public Response getView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createViewResource(viewName));
  }

  /**
   * Handles: POST /views/{viewID}
   * Create a specific view.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   *
   * @return information regarding the created view
   */
  @POST @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces("text/plain")
  public Response createView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createViewResource(viewName));
  }

  /**
   * Handles: PUT /views/{viewID}
   * Update a specific view.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   *
   * @return information regarding the updated view
   */
  @PUT @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces("text/plain")
  public Response updateView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createViewResource(viewName));
  }

  /**
   * Handles: DELETE /views/{viewID}
   * Delete a specific view.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   *
   * @return information regarding the deleted view
   */
  @DELETE @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces("text/plain")
  public Response deleteView(@Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createViewResource(viewName));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view resource.
   *
   * @param viewName view name
   *
   * @return a view resource instance
   */
  private ResourceInstance createViewResource(String viewName) {
    return createResource(Resource.Type.View,
        Collections.singletonMap(Resource.Type.View, viewName));
  }
}
