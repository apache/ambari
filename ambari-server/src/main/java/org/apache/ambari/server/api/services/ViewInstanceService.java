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
  private String m_viewName;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance service.
   *
   * @param viewName  the view id
   */
  public ViewInstanceService(String viewName) {
    m_viewName = viewName;
  }

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
  public Response getService(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("instanceName") String instanceName) {

    return handleRequest(headers, null, ui, Request.Type.GET,
        createServiceResource(m_viewName, instanceName));
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
  public Response getServices(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createServiceResource(m_viewName, null));
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
    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_viewName, instanceName));
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

    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_viewName, null));
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

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_viewName, instanceName));
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

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_viewName, null));
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

    return handleRequest(headers, null, ui, Request.Type.DELETE, createServiceResource(m_viewName, instanceName));
  }

  /**
   * Get the sub-resource
   *
   * @param instanceName  the instance id
   *
   * @return the service
   */
  @Path("{instanceName}/{resources}")
  public Object getResourceHandler(@PathParam("instanceName") String instanceName,
                                            @PathParam("resources") String resources) {

    ViewInstanceEntity instanceDefinition =
        ViewRegistry.getInstance().getInstanceDefinition(m_viewName, instanceName);

    if (instanceDefinition == null) {
      throw new IllegalArgumentException("A view instance " +
          m_viewName + "/" + instanceName + " can not be found.");
    }

    Object service = instanceDefinition.getService(resources);

    if (service == null) {
      throw new IllegalArgumentException("A resource type " + resources + " for view instance " +
          m_viewName + "/" + instanceName + " can not be found.");
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
  private ResourceInstance createServiceResource(String viewName, String instanceName) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewInstance, instanceName);
    return createResource(Resource.Type.ViewInstance, mapIds);
  }
}
