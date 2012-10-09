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

package org.apache.ambari.api.services;

import org.apache.ambari.api.resources.ClusterResourceDefinition;
import org.apache.ambari.api.resources.ResourceDefinition;

import javax.ws.rs.*;
import javax.ws.rs.core.*;


/**
 * Service responsible for cluster resource requests.
 */
@Path("/clusters/")
public class ClusterService extends BaseService {

  /**
   * Handles: GET /clusters/{clusterID}
   * Get a specific cluster.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param clusterName cluster id
   * @return cluster instance representation
   */
  @GET
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response getCluster(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("clusterName") String clusterName) {

    return handleRequest(headers, null, ui, Request.Type.GET, createResourceDefinition(clusterName));
  }

  /**
   * Handles: GET  /clusters
   * Get all clusters.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return cluster collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getClusters(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResourceDefinition(null));
  }

  /**
   * Handles: PUT /clusters/{clusterID}
   * Create a specific cluster.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param clusterName cluster id
   * @return information regarding the created cluster
   */
  @PUT
  @Produces("text/plain")
  public Response createCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResourceDefinition(clusterName));
  }

  /**
   * Handles: POST /clusters/{clusterID}
   * Update a specific cluster.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param clusterName cluster id
   * @return information regarding the updated cluster
   */
  @POST
  @Produces("text/plain")
  public Response updateCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createResourceDefinition(clusterName));
  }

  /**
   * Handles: DELETE /clusters/{clusterID}
   * Delete a specific cluster.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param clusterName cluster id
   * @return information regarding the deleted cluster
   */
  @DELETE
  @Produces("text/plain")
  public Response deleteCluster(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResourceDefinition(clusterName));
  }

  /**
   * Get the hosts sub-resource
   *
   * @param clusterName cluster id
   * @return the hosts service
   */
  @Path("{clusterName}/hosts")
  public HostService getHostHandler(@PathParam("clusterName") String clusterName) {
    return new HostService(clusterName);
  }

  /**
   * Get the services sub-resource
   *
   * @param clusterName cluster id
   * @return the services service
   */
  @Path("{clusterName}/services")
  public ServiceService getServiceHandler(@PathParam("clusterName") String clusterName) {
    return new ServiceService(clusterName);
  }

  /**
   * Create a cluster resource definition.
   *
   * @param clusterName cluster name
   * @return a cluster resource definition
   */
  ResourceDefinition createResourceDefinition(String clusterName) {
    return new ClusterResourceDefinition(clusterName);
  }
}
