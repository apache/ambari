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

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Endpoint for alert definitions.
 */
public class AlertDefinitionService extends BaseService {

  private String clusterName = null;

  AlertDefinitionService(String clusterName) {
    this.clusterName = clusterName;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getDefinitions(@Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
      createResourceInstance(clusterName, null));
  }

  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createDefinition(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createResourceInstance(clusterName, null));
  }

  @PUT @ApiIgnore // until documented
  @Path("{alertDefinitionId}")
  @Produces("text/plain")
  public Response updateDefinition(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("alertDefinitionId") Long id) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
      createResourceInstance(clusterName, id));
  }

  @DELETE @ApiIgnore // until documented
  @Path("{alertDefinitionId}")
  @Produces("text/plain")
  public Response deleteDefinition(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("alertDefinitionId") Long id) {
    return handleRequest(headers, body, ui, Request.Type.DELETE,
      createResourceInstance(clusterName, id));
  }

  @GET @ApiIgnore // until documented
  @Path("{alertDefinitionId}")
  @Produces("text/plain")
  public Response getDefinitions(@Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("alertDefinitionId") Long id) {
    return handleRequest(headers, null, ui, Request.Type.GET,
      createResourceInstance(clusterName, id));
  }

  /**
   * Create a request schedule resource instance
   */
  private ResourceInstance createResourceInstance(String clusterName,
      Long definitionId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.AlertDefinition, null == definitionId ? null : definitionId.toString());

    return createResource(Resource.Type.AlertDefinition, mapIds);
  }

}
