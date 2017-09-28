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
import org.apache.ambari.server.controller.ViewPermissionResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for custom view permission resource requests.
 */
@Path("/views/{viewName}/versions/{version}/permissions")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewPermissionService extends BaseService {

  /**
   * Handles: GET  /permissions
   * Get all permissions.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    version id
   *
   * @return permission collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all permissions for a view", nickname = "ViewPermissionService#getPermissions", notes = "Returns all permission details for the version of a view.", response = ViewPermissionResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter privileges", defaultValue = "PermissionInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewPermissionResponse.class, responseContainer = "List")}
  )
  public Response getPermissions(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @ApiParam(value = "view version") @PathParam("version") String version) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(
      viewName, version, null));
  }

  /**
   * Handles: GET /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Get a specific permission.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param viewName       view id
   * @param version        version id
   * @param permissionId   permission id
   *
   * @return permission instance representation
   */
  @GET
  @Path("{permissionId}")
  @Produces("text/plain")
  @ApiOperation(value = "Get single view permission", nickname = "ViewPermissionService#getPermission", notes = "Returns permission details for a single version of a view.", response = ViewPermissionResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view permission details", defaultValue = "PermissionInfo", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewPermissionResponse.class)}
  )
  public Response getPermission(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                @ApiParam(value = "view version") @PathParam("version") String version,
                                @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Create a specific permission.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param viewName       view id
   * @param version        version id
   * @param permissionId   permission id
   *
   * @return information regarding the created permission
   */
  @POST @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response createPermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.POST, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Update a specific permission.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param permissionId  permission id
   * @return information regarding the updated permission
   */
  @PUT @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response updatePermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Delete a specific permission.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param permissionId  permission id
   *
   * @return information regarding the deleted permission
   */
  @DELETE @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response deletePermission(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createPermissionResource(
        viewName, version, permissionId));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a permission resource.
   *
   * @param permissionId permission name
   *
   * @return a permission resource instance
   */
  protected ResourceInstance createPermissionResource(String viewName, String viewVersion, String permissionId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, viewVersion);
    mapIds.put(Resource.Type.ViewPermission, permissionId);

    return createResource(Resource.Type.ViewPermission, mapIds);
  }
}
