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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.view.ViewRegistry;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for instances resource requests.
 */
public class ViewInstanceService extends BaseService {
  /**
   * Parent view name.
   */
  private final String viewName;

  /**
   * The view version.
   */
  private final String version;

  /**
   * The view registry;
   */
  private final ViewRegistry viewRegistry;

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance service.
   *
   * @param viewName  the view id
   * @param version   the version
   */
  public ViewInstanceService(String viewName, String version) {
    this.viewName = viewName;
    this.version = version;

    viewRegistry = ViewRegistry.getInstance();
  }


  // ----- ViewInstanceService -----------------------------------------------

  /**
   * Handles URL: /views/{viewID}/instances/{instanceID}
   * Get a specific instance.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param instanceName  instance id
   *
   * @return instance resource representation
   */
  @GET
  @Path("{instanceName}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version, instanceName));
  }

  /**
   * Handles URL: /views/{viewID}/instances
   * Get all instances for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return instance collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version, null));
  }

  /**
   * Handles: POST /views/{viewID}/instances/{instanceId}
   * Create a specific instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param instanceName  instance id
   *
   * @return information regarding the created instance
   */
  @POST
  @Path("{instanceName}")
  @Produces("text/plain")
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version, instanceName));
  }

  /**
   * Handles: POST /views/{viewID}/instances
   * Create multiple instances.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return information regarding the created instances
   */
  @POST
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version, null));
  }

  /**
   * Handles: PUT /views/{viewID}/instances/{instanceId}
   * Update a specific instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param instanceName  instance id
   *
   * @return information regarding the updated instance
   */
  @PUT
  @Path("{instanceName}")
  @Produces("text/plain")
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("instanceName") String instanceName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version, instanceName));
  }

  /**
   * Handles: PUT /views/{viewID}/instances
   * Update multiple instances.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return information regarding the updated instance
   */
  @PUT
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version, null));
  }

  /**
   * Handles: DELETE /views/{viewID}/instances/{instanceId}
   * Delete a specific instance.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param instanceName  instance id
   *
   * @return information regarding the deleted instance
   */
  @DELETE
  @Path("{instanceName}")
  @Produces("text/plain")
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("instanceName") String instanceName) throws AuthorizationException {
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
                                   @PathParam("instanceName") String instanceName,
                                   @PathParam("resources") String resources) {

    hasPermission(Request.Type.valueOf(request.getMethod()), instanceName);

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


  /**
   * Gets the admin privilege service
   */
  @Path("{instanceName}/privileges")
  public PrivilegeService getPrivilegeService(@Context javax.ws.rs.core.Request request,
                                              @PathParam ("instanceName") String instanceName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), instanceName);

    return new ViewPrivilegeService(viewName, version, instanceName);
  }

  @Path("{instanceName}/migrate")
  public ViewDataMigrationService migrateData(@Context javax.ws.rs.core.Request request,
                                              @PathParam ("instanceName") String instanceName) {
    return new ViewDataMigrationService(viewName, version, instanceName);
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
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
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
  private void hasPermission(Request.Type requestType, String instanceName) {
    if (!viewRegistry.checkPermission(viewName, version, instanceName, requestType == Request.Type.GET)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
  }
}
