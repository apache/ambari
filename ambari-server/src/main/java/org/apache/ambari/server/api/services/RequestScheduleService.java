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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
 * Service responsible for management of a batch of requests with attached
 * schedule
 */
public class RequestScheduleService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor
   * @param m_clusterName
   */
  public RequestScheduleService(String m_clusterName) {
    this.m_clusterName = m_clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterId}/request_schedules
   * Get all the scheduled requests for a cluster.
   *
   * @param headers
   * @param ui
   * @return
   */
  @GET
  @Produces("text/plain")
  public Response getRequestSchedules(String body,
                                      @Context HttpHeaders headers,
                                      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRequestSchedule(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterId}/request_schedules/{requestScheduleId}
   * Get details on a specific request schedule
   *
   * @return
   */
  @GET
  @Path("{requestScheduleId}")
  @Produces("text/plain")
  public Response getRequestSchedule(String body,
                                     @Context HttpHeaders headers,
                                     @Context UriInfo ui,
                                     @PathParam("requestScheduleId") String requestScheduleId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRequestSchedule(m_clusterName, requestScheduleId));
  }

  /**
   * Handles POST /clusters/{clusterId}/request_schedules
   * Create a new request schedule
   *
   * @param body
   * @param headers
   * @param ui
   * @return
   */
  @POST
  @Produces("text/plain")
  public Response createRequestSchedule(String body,
                                        @Context HttpHeaders headers,
                                        @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createRequestSchedule(m_clusterName, null));
  }

  /**
   * Handles DELETE /clusters/{clusterId}/request_schedules/{requestScheduleId}
   * Delete a request schedule
   *
   * @param headers
   * @param ui
   * @param requestScheduleId
   * @return
   */
  @DELETE
  @Path("{requestScheduleId}")
  @Produces("text/plain")
  public Response deleteRequestSchedule(@Context HttpHeaders headers,
                                        @Context UriInfo ui,
                                        @PathParam("requestScheduleId") String requestScheduleId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE,
      createRequestSchedule(m_clusterName, requestScheduleId));
  }



  /**
   * Create a request schedule resource instance
   * @param clusterName
   * @param requestScheduleId
   * @return
   */
  private ResourceInstance createRequestSchedule(String clusterName,
                                                 String requestScheduleId) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.RequestSchedule, requestScheduleId);

    return createResource(Resource.Type.RequestSchedule, mapIds);
  }

}
