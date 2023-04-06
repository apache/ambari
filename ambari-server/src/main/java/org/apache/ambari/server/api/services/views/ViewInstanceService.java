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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.controller.ViewInstanceResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.view.ViewRegistry;

import org.apache.http.HttpStatus;

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

  public static final String VIEW_INSTANCE_REQUEST_TYPE = "org.apache.ambari.server.controller.ViewInstanceResponse";

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
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all view instances", response = ViewInstanceResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewInstanceInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
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
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view instance", response = ViewInstanceResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewInstanceInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
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
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create view instance")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_INSTANCE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
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
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create view instances")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_INSTANCE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
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
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update view instance detail")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_INSTANCE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
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
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update multiple view instance detail")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_INSTANCE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
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
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete view instance")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
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
      String msg = "A view instance " +
          viewName + "/" + instanceName + " can not be found.";

      return new NotFoundResponse(msg);
    }

    Object service = instanceDefinition.getService(resources);

    if (service == null) {
      String msg = "A resource type " + resources + " for view instance " +
          viewName + "/" + instanceName + " can not be found.";
      return new NotFoundResponse(msg);
    }
    return service;
  }

  /**
   * Stub class for 404 error response
   *
   */
  @Path("/")
  public class NotFoundResponse {

    String msg;

    NotFoundResponse(String msg){
      this.msg=msg;
    }

    /**
     * Handles: GET /{resourceName}
     * Handle GET resource with 404 response
     * @return 404 response with msg
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle GET resource with 404 response")
    public Response get() {
      return getResponse();
    }

    /**
     * Handles: POST /{resourceName}
     * Handle POST resource with 404 response
     * @return 404 response with msg
     */
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle POST resource with 404 response")
    public Response post() {
      return getResponse();
    }

    /**
     * Handles: PUT /{resourceName}
     * Handle PUT resource with 404 response
     * @return 404 response with msg
     */
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle PUT resource with 404 response")
    public Response put() {
      return getResponse();
    }

    /**
     * Handles: DELETE /{resourceName}
     * Handle DELETE resource with 404 response
     * @return 404 response with msg
     */
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle DELETE resource with 404 response")
    public Response delete() {
      return getResponse();
    }

    /**
     * Handles: GET /{resourceName}/{.*}
     * Handle GET sub-resource with 404 response
     * @return 404 response with msg
     */
    @GET
    @Path("{path: .*}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle GET sub-resource with 404 response")
    public Response getSub() {
      return getResponse();
    }

    /**
     * Handles: POST /{resourceName}/{.*}
     * Handle POST sub-resource with 404 response
     * @return 404 response with msg
     */
    @POST
    @Path("{path: .*}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle POST sub-resource with 404 response")
    public Response postSub() {
      return getResponse();
    }

    /**
     * Handles: PUT /{resourceName}/{.*}
     * Handle PUT sub-resource with 404 response
     * @return 404 response with msg
     */
    @PUT
    @Path("{path: .*}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle PUT sub-resource with 404 response")
    public Response putSub() {
      return getResponse();
    }

    /**
     * Handles: DELETE /{resourceName}/{.*}
     * Handle DELETE sub-resource with 404 response
     * @return 404 response with msg
     */
    @DELETE
    @Path("{path: .*}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Handle DELETE sub-resource with 404 response")
    public Response deleteSub() {
      return getResponse();
    }

    /**
     * Build 404 response with msg
     * @return 404 response with msg
     */
    public Response getResponse() {
      Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, msg));
      Response.ResponseBuilder builder = Response.status(result.getStatus().getStatusCode()).entity(new JsonSerializer().serialize(result));
      return builder.build();
    }

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
