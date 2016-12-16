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


import java.util.HashMap;
import java.util.Map;

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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * Service responsible for request resource requests.
 */
@Path("/requests/")
public class RequestService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;


  public RequestService() {
  }
  
  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public RequestService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/requests/{requestID} or
   * /requests/{requestId}
   * Get a specific request.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param requestId  request id
   *
   * @return request resource representation
   */
  @GET
  @Path("{requestId}")
  @Produces("text/plain")
  public Response getRequest(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("requestId") String requestId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createRequestResource(m_clusterName, requestId));
  }

  /**
   * Handles URL: /clusters/{clusterId}/requests or /requests
   * Get all requests for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return request collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createRequestResource(m_clusterName, null));
  }

  /**
   * Gets the stage sub-resource.
   */
  @Path("{requestId}/stages")
  public StageService getStageHandler(@PathParam("requestId") String requestId) {
    return new StageService(m_clusterName, requestId);
  }

  /**
   * Gets the tasks sub-resource.
   */
  @Path("{requestId}/tasks")
  public TaskService getTaskHandler(@PathParam("requestId") String requestId) {
    return new TaskService(m_clusterName, requestId, null);
  }

  /**
   * Handles: PUT /clusters/{clusterId}/requests/{requestId} or /requests/{requestId}
   * Change state of existing requests. Usually used to cancel running requests
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the updated requests
   */
  @PUT
  @Path("{requestId}")
  @Produces("text/plain")
  public Response updateRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("requestId") String requestId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createRequestResource(m_clusterName, requestId));
  }

  /**
   * Handles: POST /clusters/{clusterId}/requests or /requests
   * Create multiple services.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created requests
   */
  @POST
  @Produces("text/plain")
  public Response createRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST, createRequestResource(m_clusterName, null));
  }

  /**
   * Create a request resource instance.
   *
   * @param clusterName  cluster name
   * @param requestId    request id
   *
   * @return a request resource instance
   */
  ResourceInstance createRequestResource(String clusterName, String requestId) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    
    if (null != clusterName)
      mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Request, requestId);

    return createResource(Resource.Type.Request, mapIds);
  }
}
