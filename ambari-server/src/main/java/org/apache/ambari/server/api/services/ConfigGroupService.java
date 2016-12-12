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
 * Service responsible for management of Config Groups
 */
public class ConfigGroupService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor
   * @param m_clusterName
   */
  public ConfigGroupService(String m_clusterName) {
    this.m_clusterName = m_clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterId}/config_groups
   * Get all the config groups for a cluster.
   *
   * @param headers
   * @param ui
   * @return
   */
  @GET
  @Produces("text/plain")
  public Response getConfigGroups(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createConfigGroupResource(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterId}/config_groups/{groupId}
   * Get details on a config group.
   *
   * @return
   */
  @GET
  @Path("{groupId}")
  @Produces("text/plain")
  public Response getConfigGroup(String body, @Context HttpHeaders headers,
          @Context UriInfo ui, @PathParam("groupId") String groupId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createConfigGroupResource(m_clusterName, groupId));
  }

  /**
   * Handles POST /clusters/{clusterId}/config_groups
   * Create a new config group
   *
   * @param body
   * @param headers
   * @param ui
   * @return
   */
  @POST
  @Produces("text/plain")
  public Response createConfigGroup(String body, @Context HttpHeaders headers,
                                    @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createConfigGroupResource(m_clusterName, null));
  }

  /**
   * Handles PUT /clusters/{clusterId}/config_groups/{groupId}
   * Update a config group
   *
   * @param body
   * @param headers
   * @param ui
   * @param groupId
   * @return
   */
  @PUT
  @Path("{groupId}")
  @Produces("text/plain")
  public Response updateConfigGroup(String body, @Context HttpHeaders
    headers, @Context UriInfo ui, @PathParam("groupId") String groupId) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
      createConfigGroupResource(m_clusterName, groupId));
  }

  /**
   * Handles DELETE /clusters/{clusterId}/config_groups/{groupId}
   * Delete a config group
   *
   * @param headers
   * @param ui
   * @param groupId
   * @return
   */
  @DELETE
  @Path("{groupId}")
  @Produces("text/plain")
  public Response deleteConfigGroup(@Context HttpHeaders headers,
                                    @Context UriInfo ui,
                                    @PathParam("groupId") String groupId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE,
      createConfigGroupResource(m_clusterName, groupId));
  }


  /**
   * Create a request resource instance.
   *
   * @param clusterName  cluster name
   * @param groupId    config group id
   *
   * @return a request resource instance
   */
  ResourceInstance createConfigGroupResource(String clusterName,
                                             String groupId) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ConfigGroup, groupId);

    return createResource(Resource.Type.ConfigGroup, mapIds);
  }
}
