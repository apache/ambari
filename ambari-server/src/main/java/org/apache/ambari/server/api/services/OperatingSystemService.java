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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * The {@link OperatingSystemService} is a sub resource off of
 * {@link MpacksService} which provides the ability to expose and update
 * repositories which ship with management packs.
 */
public class OperatingSystemService extends BaseService {

  /**
   * The parent of each OS resource.
   */
  private final String m_mpackId;

  /**
   * Constructor.
   *
   * @param parentKeyProperties
   *          extra properties to be inserted into created resource
   */
  public OperatingSystemService(String mpackId) {
    m_mpackId = mpackId;
  }

  /**
   * Gets all operating systems. Handles: GET /operating_systems requests.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   */
  @GET
  @ApiIgnore
  @Produces("text/plain")
  public Response getOperatingSystems(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(null));
  }

  /**
   * Gets a single operating system. Handles: GET /operating_systems/{osType}
   * requests.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param osType
   *          os type
   * @return information regarding the specified operating system
   */
  @GET
  @ApiIgnore
  @Path("{osType}")
  @Produces("text/plain")
  public Response getOperatingSystem(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("osType") String osType) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(osType));
  }

  /**
   * Creates the repositories and properties of a specified operating system.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param osType
   *          os type
   * @return information regarding the specified operating system
   */
  @POST
  @ApiIgnore
  @Path("{osType}")
  @Produces("text/plain")
  public Response createOperatingSystem(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("osType") String osType) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(osType));
  }

  /**
   * Updates the repositories and properties of a specified operating system.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param osType
   *          os type
   * @return information regarding the specified operating system
   */
  @PUT
  @ApiIgnore
  @Path("{osType}")
  @Produces("text/plain")
  public Response updateOperatingSystem(String body, @Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("osType") String osType) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(osType));
  }

  /**
   * Removes the specified operating system.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param osType
   *          os type
   * @return the delete request status
   */
  @DELETE
  @ApiIgnore
  @Path("{osType}")
  @Produces("text/plain")
  public Response deleteOperatingSystem(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("osType") String osType) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(osType));
  }

  /**
   * Create an operating system resource instance.
   *
   * @param osType
   *          os type
   *
   * @return an operating system instance
   */
  private ResourceInstance createResource(String osType) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Mpack, m_mpackId);
    mapIds.put(Resource.Type.OperatingSystem, osType);
    return createResource(Resource.Type.OperatingSystem, mapIds);
  }
}
