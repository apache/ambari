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

import java.util.Collections;

import javax.ws.rs.GET;
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
 * Service responsible for controllers.
 */
@Path("/controllers/")
public class ControllerService extends BaseService {
  /**
   * Handles: GET  /controllers
   * Get all controllers.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return controller collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getControllers(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createControllerResource(null));
  }

  /**
   * Handles: GET  /controllers/{controllerName}
   * Get single controller.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return controller resource representation
   */
  @GET
  @Path("{controllerName}")
  @Produces("text/plain")
  public Response getController(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("controllerName") String controllerName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createControllerResource(controllerName));
  }

  /**
   * Handles: PUT  /controllers/{controllerName}
   * Update data of a single controller.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return controller resource representation
   */
  @PUT
  @Path("{controllerName}")
  @Produces("text/plain")
  public Response updateController(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("controllerName") String controllerName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createControllerResource(controllerName));
  }

  /**
   * Create a controller resource instance.
   *
   * @param controllerName controller name
   *
   * @return a cluster resource instance
   */
  ResourceInstance createControllerResource(String controllerName) {
    return createResource(Resource.Type.Controller,
        Collections.singletonMap(Resource.Type.Controller, controllerName));
  }
}
