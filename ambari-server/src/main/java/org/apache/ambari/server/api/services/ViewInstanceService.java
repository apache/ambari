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
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
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
    this.version  = version;

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
                             @PathParam("instanceName") String instanceName) {

    hasPermission(Request.Type.GET, instanceName);
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResource(viewName, version, instanceName));
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
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    hasPermission(Request.Type.GET, null);
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResource(viewName, version,  null));
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
                                @PathParam("instanceName") String instanceName) {
    hasPermission(Request.Type.POST, instanceName);
    return handleRequest(headers, body, ui, Request.Type.POST,
        createResource(viewName, version,  instanceName));
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
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    hasPermission(Request.Type.POST, null);
    return handleRequest(headers, body, ui, Request.Type.POST,
        createResource(viewName, version,  null));
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
                                @PathParam("instanceName") String instanceName) {

    hasPermission(Request.Type.PUT, instanceName);
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version,  instanceName));
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
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    hasPermission(Request.Type.PUT, null);
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version,  null));
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
                                @PathParam("instanceName") String instanceName) {

    hasPermission(Request.Type.DELETE, instanceName);
    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(viewName, version,  instanceName));
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

  /**
   * Gets the admin privilege service
   */
  @Path("{instanceName}/privileges")
  public PrivilegeService getPrivilegeService(@Context javax.ws.rs.core.Request request,
                                              @PathParam ("instanceName") String instanceName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), instanceName);

    return new ViewPrivilegeService(viewName, version, instanceName);
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
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
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
