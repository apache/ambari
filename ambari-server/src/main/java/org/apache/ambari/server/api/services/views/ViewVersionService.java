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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.ViewVersionResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for view version resource requests.
 */
@Path("/views/{viewName}/versions")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewVersionService extends BaseService {

  /**
   * Handles: GET  /views/{viewName}/versions
   * Get all views versions.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param viewName   view id
   *
   * @return view collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all versions for a view", nickname = "ViewVersionService#getVersions", notes = "Returns details of all versions for a view.", response = ViewVersionResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view version details", defaultValue = "ViewVersionInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort users (asc | desc)", defaultValue = "ViewVersionInfo/version.desc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewVersionResponse.class, responseContainer = "List")}
  )
  public Response getVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                              @ApiParam(value = "view name") @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, null));
  }

  /**
   * Handles: GET /views/{viewName}/versions/{version}
   * Get a specific view version.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param viewName   view id
   * @param version  version id
   *
   * @return view instance representation
   */
  @GET
  @Path("{version}")
  @Produces("text/plain")
  @ApiOperation(value = "Get single view version", nickname = "ViewVersionService#getVersion", notes = "Returns view details.", response = ViewVersionResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view details", defaultValue = "ViewVersionInfo", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewVersionResponse.class)}
  )
  public Response getVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                              @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                              @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}
   * Create a specific view version.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    the version
   *
   * @return information regarding the created view
   */
  @POST @ApiIgnore // until documented
  @Path("{version}")
  @Produces("text/plain")
  public Response createVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}
   * Update a specific view version.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   * @param version   the version
   *
   * @return information regarding the updated view
   */
  @PUT @ApiIgnore // until documented
  @Path("{version}")
  @Produces("text/plain")
  public Response updateVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}
   * Delete a specific view version.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName   view id
   * @param version   version id
   *
   * @return information regarding the deleted view version
   */
  @DELETE @ApiIgnore // until documented
  @Path("{version}")
  @Produces("text/plain")
  public Response deleteVersions(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("viewName") String viewName, @PathParam("version") String version) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(viewName, version));
  }

  /**
   * Get the permissions sub-resource
   *
   * @param version  the version
   *
   * @return the permission service

  @Path("{version}/permissions")
  public ViewPermissionService getPermissionHandler(@PathParam("version") String version) {

    return new ViewPermissionService(viewName, version);
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view resource.
   *
   * @param viewName view name
   *
   * @return a view resource instance
   */
  private ResourceInstance createResource(String viewName, String version) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, version);
    return createResource(Resource.Type.ViewVersion, mapIds);
  }
}
