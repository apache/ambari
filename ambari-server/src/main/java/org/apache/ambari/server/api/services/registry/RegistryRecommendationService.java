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

package org.apache.ambari.server.api.services.registry;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * REST API endpoint for registry recommendations
 */
@Path("/registries/{registryId}/recommendations")
public class RegistryRecommendationService extends BaseService {

  /**
   * Create registry recommendation
   * @param body        Request body
   * @param headers     Http headers
   * @param ui          Uri info
   * @param registryId  Registry id
   * @return            {@link Response} containing registry recommendation response
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiIgnore // until documented
  public Response createRegistryRecommendation(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @PathParam("registryId") String registryId) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createRegistryRecommendationResource(registryId));
  }

  /**
   * Create registry recommendation resource instance
   * @param registryId  Registry id
   * @return            Registry recommendation resource instance
   */
  ResourceInstance createRegistryRecommendationResource(String registryId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Registry, registryId);
    return createResource(Resource.Type.RegistryRecommendation, mapIds);
  }
}
