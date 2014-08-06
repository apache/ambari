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

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for services resource requests.
 */
public class ServiceService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public ServiceService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/services/{serviceID}
   * Get a specific service.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return service resource representation
   */
  @GET
  @Path("{serviceName}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles URL: /clusters/{clusterId}/services
   * Get all services for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: POST /clusters/{clusterId}/services/{serviceId}
   * Create a specific service.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the created service
   */
  @POST
  @Path("{serviceName}")
  @Produces("text/plain")
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles: POST /clusters/{clusterId}/services
   * Create multiple services.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created services
   */
  @POST
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: PUT /clusters/{clusterId}/services/{serviceId}
   * Update a specific service.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the updated service
   */
  @PUT
  @Path("{serviceName}")
  @Produces("text/plain")
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles: PUT /clusters/{clusterId}/services
   * Update multiple services.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the updated service
   */
  @PUT
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: DELETE /clusters/{clusterId}/services/{serviceId}
   * Delete a specific service.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the deleted service
   */
  @DELETE
  @Path("{serviceName}")
  @Produces("text/plain")
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Get the components sub-resource.
   *
   * @param serviceName service id
   * @return the components service
   */
  @Path("{serviceName}/components")
  public ComponentService getComponentHandler(@PathParam("serviceName") String serviceName) {

    return new ComponentService(m_clusterName, serviceName);
  }

  /**
   * Create a service resource instance.
   *
   *
   * @param clusterName  cluster name
   * @param serviceName  service name
   *
   * @return a service resource instance
   */
  ResourceInstance createServiceResource(String clusterName, String serviceName) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Service, serviceName);

    return createResource(Resource.Type.Service, mapIds);
  }
}
