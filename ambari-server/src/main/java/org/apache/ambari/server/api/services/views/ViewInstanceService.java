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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.ViewInstanceResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.view.ViewRegistry;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for instances resource requests.
 */
@Path("/views/{viewName}/versions/{version}/instances")
@Api(tags = "Views", description = "Endpoint for view specific operations")
public class ViewInstanceService extends BaseService {
  /**
   * The view registry;
   */
  private final ViewRegistry viewRegistry = ViewRegistry.getInstance();

  /**
   * Handles URL: /views/{viewName}/versions/{version}/instances
   * Get all instances for a view.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    version id
   *
   * @return instance collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all view instances", nickname = "ViewInstanceService#getServices", notes = "Returns all instances for a view version.", response = ViewInstanceResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view instance details", defaultValue = "ViewInstanceInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort users (asc | desc)", defaultValue = "ViewInstanceInfo/instance_name.desc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewInstanceResponse.class, responseContainer = "List")}
  )
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                              @PathParam("viewName") String viewName, @PathParam("version") String version) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version, null));
  }

  /**
   * Handles URL: /views/{viewName}/versions/{version}/instances/{instanceID}
   * Get a specific instance.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return instance resource representation
   */
  @GET
  @Path("{instanceName}")
  @Produces("text/plain")
  @ApiOperation(value = "Get single view instance", nickname = "ViewInstanceService#getService", notes = "Returns view instance details.", response = ViewInstanceResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter view instance details", defaultValue = "ViewInstanceInfo", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = ViewInstanceResponse.class)}
  )
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "view name") @PathParam("viewName") String viewName, @PathParam("version") String version,
                             @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version, instanceName));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}/instances/{instanceId}
   * Create a specific instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return information regarding the created instance
   */
  @POST
  @Path("{instanceName}")
  @Produces("text/plain")
  @ApiOperation(value = "Create view instance", nickname = "ViewInstanceService#createService", notes = "Creates view instance resource.")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.ViewInstanceRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName, @PathParam("version") String version,
                                @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version, instanceName));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}/instances
   * Create multiple instances.
   *
   * @param body       http body
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    version id
   *
   * @return information regarding the created instances
   */
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("viewName") String viewName, @PathParam("version") String version) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version, null));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/instances/{instanceId}
   * Update a specific instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName   view id
   * @param version    version id
   * @param instanceName  instance id
   *
   * @return information regarding the updated instance
   */
  @PUT
  @Path("{instanceName}")
  @Produces("text/plain")
  @ApiOperation(value = "Update view instance detail", nickname = "ViewInstanceService#updateService", notes = "Updates view instance resource.")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.ViewInstanceRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName, @PathParam("version") String version,
                                @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version, instanceName));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/instances
   * Update multiple instances.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param viewName   view id
   * @param version    version id
   *
   * @return information regarding the updated instance
   */
  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("viewName") String viewName, @PathParam("version") String version) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version, null));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}/instances/{instanceId}
   * Delete a specific instance.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName   view id
   * @param version    version id
   * @param instanceName  instance id
   *
   * @return information regarding the deleted instance
   */
  @DELETE
  @Path("{instanceName}")
  @Produces("text/plain")
  @ApiOperation(value = "Delete view instance", nickname = "ViewInstanceService#deleteService", notes = "Delete view resource.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName, @PathParam("version") String version,
                                @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(viewName, version, instanceName));
  }

  /**
   * Get the sub-resource
   *
   * @param instanceName  the instance id
   *
   * @return the service
   */
  @Path("{instanceName}/{resources}")
  public Object getResourceHandler(@Context javax.ws.rs.core.Request request,
                                   @PathParam("viewName") String viewName, @PathParam("version") String version,
                                   @PathParam("instanceName") String instanceName,
                                   @PathParam("resources") String resources) {

    hasPermission(viewName, version, Request.Type.valueOf(request.getMethod()), instanceName);

    ViewInstanceEntity instanceDefinition =
        ViewRegistry.getInstance().getInstanceDefinition(viewName, version, instanceName);

    if (instanceDefinition == null) {
      throw new IllegalArgumentException("A view instance " +
          viewName + "/" + instanceName + " can not be found.");
    }

    Object service = instanceDefinition.getService(resources);

    if (service == null) {
      throw new IllegalArgumentException("A resource type " + resources + " for view instance " +
          viewName + "/" + instanceName + " can not be found.");
    }
    return service;
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create an view instance resource.
   *
   * @param viewName      view name
   * @param instanceName  instance name
   *
   * @return a view instance resource
   */
  private ResourceInstance createResource(String viewName, String viewVersion, String instanceName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, viewVersion);
    mapIds.put(Resource.Type.ViewInstance, instanceName);
    return createResource(Resource.Type.ViewInstance, mapIds);
  }

  /**
   * Determine whether or not the access specified by the given request type
   * is permitted for the current user on the view instance resource identified
   * by the given instance name.
   *
   * @param requestType   the request method type
   * @param instanceName  the name of the view instance resource
   *
   * @throws WebApplicationException if access is forbidden
   */
  private void hasPermission(String viewName, String version, Request.Type requestType, String instanceName) {
    if (!viewRegistry.checkPermission(viewName, version, instanceName, requestType == Request.Type.GET)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
  }
}
