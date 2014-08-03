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
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Clusters;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Collections;


/**
 * Service responsible for cluster resource requests.
 */
@Path("/clusters/")
public class ClusterService extends BaseService {

  /**
   * The clusters utilities.
   */
  private final Clusters clusters;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a ClusterService.
   */
  public ClusterService() {
    this.clusters = AmbariServer.getController().getClusters();
  }

  /**
   * Construct a ClusterService.
   *
   * @param clusters  the clusters utilities
   */
  protected ClusterService(Clusters clusters) {
    this.clusters = clusters;
  }


  // ----- ClusterService ----------------------------------------------------

  /**
   * Handles: GET /clusters/{clusterID}
   * Get a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return cluster instance representation
   */
  @GET
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response getCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.GET, clusterName);
    return handleRequest(headers, body, ui, Request.Type.GET, createClusterResource(clusterName));
  }

  /**
   * Handles: GET  /clusters
   * Get all clusters.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return cluster collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getClusters(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    hasPermission(Request.Type.GET, null);
    return handleRequest(headers, body, ui, Request.Type.GET, createClusterResource(null));
  }

  /**
   * Handles: POST /clusters/{clusterID}
   * Create a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the created cluster
   */
   @POST
   @Path("{clusterName}")
   @Produces("text/plain")
   public Response createCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("clusterName") String clusterName) {

     hasPermission(Request.Type.POST, clusterName);
     return handleRequest(headers, body, ui, Request.Type.POST, createClusterResource(clusterName));
  }

  /**
   * Handles: PUT /clusters/{clusterID}
   * Update a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the updated cluster
   */
  @PUT
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response updateCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.PUT, clusterName);
    return handleRequest(headers, body, ui, Request.Type.PUT, createClusterResource(clusterName));
  }

  /**
   * Handles: DELETE /clusters/{clusterID}
   * Delete a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the deleted cluster
   */
  @DELETE
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response deleteCluster(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.DELETE, clusterName);
    return handleRequest(headers, null, ui, Request.Type.DELETE, createClusterResource(clusterName));
  }

  /**
   * Get the hosts sub-resource
   *
   * @param request      the request
   * @param clusterName  cluster id
   *
   * @return the hosts service
   */
  @Path("{clusterName}/hosts")
  public HostService getHostHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new HostService(clusterName);
  }

  /**
   * Get the services sub-resource
   *
   * @param request      the request
   * @param clusterName  cluster id
   *
   * @return the services service
   */
  @Path("{clusterName}/services")
  public ServiceService getServiceHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new ServiceService(clusterName);
  }
  
  /**
   * Gets the configurations sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return the configuration service
   */
  @Path("{clusterName}/configurations")
  public ConfigurationService getConfigurationHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new ConfigurationService(clusterName);
  }

  /**
   * Gets the requests sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return the requests service
   */
  @Path("{clusterName}/requests")
  public RequestService getRequestHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new RequestService(clusterName);
  }

  /**
   * Get the host component resource without specifying the parent host component.
   * Allows accessing host component resources across hosts.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the host component service with no parent set
   */
  @Path("{clusterName}/host_components")
  public HostComponentService getHostComponentHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new HostComponentService(clusterName, null);
  }

  /**
   * Get the component resource without specifying the parent service.
   * Allows accessing component resources across services.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the host component service with no parent set
   */
  @Path("{clusterName}/components")
  public ComponentService getComponentHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new ComponentService(clusterName, null);
  }

  /**
   * Gets the workflows sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the workflow service
   */
  @Path("{clusterName}/workflows")
  public WorkflowService getWorkflowHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new WorkflowService(clusterName);
  }

  /**
   * Gets the config group service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the config group service
   */
  @Path("{clusterName}/config_groups")
  public ConfigGroupService getConfigGroupService(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new ConfigGroupService(clusterName);
  }

  /**
   * Gets the request schedule service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the request schedule service
   */
  @Path("{clusterName}/request_schedules")
  public RequestScheduleService getRequestScheduleService
                             (@Context javax.ws.rs.core.Request request, @PathParam ("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new RequestScheduleService(clusterName);
  }
  
  /**
   * Gets the alert definition service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the alert definition service
   */
  @Path("{clusterName}/alert_definitions")
  public AlertDefinitionService getAlertDefinitionService(
      @Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new AlertDefinitionService(clusterName);
  }

  /**
   * Gets the privilege service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the privileges service
   */
  @Path("{clusterName}/privileges")
  public PrivilegeService getPrivilegeService(@Context javax.ws.rs.core.Request request, @PathParam ("clusterName") String clusterName) {

    hasPermission(Request.Type.valueOf(request.getMethod()), clusterName);
    return new ClusterPrivilegeService(clusterName);
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create a cluster resource instance.
   *
   * @param clusterName cluster name
   *
   * @return a cluster resource instance
   */
  ResourceInstance createClusterResource(String clusterName) {
    return createResource(Resource.Type.Cluster,
        Collections.singletonMap(Resource.Type.Cluster, clusterName));
  }

  /**
   * Determine whether or not the access specified by the given request type
   * is permitted for the current user on the cluster resource identified by
   * the given cluster name.
   *
   * @param requestType  the request method type
   * @param clusterName  the name of the cluster resource
   *
   * @throws WebApplicationException if access is forbidden
   */
  private void hasPermission(Request.Type requestType, String clusterName) throws WebApplicationException {
    if (!clusters.checkPermission(clusterName, requestType == Request.Type.GET)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
  }
}
