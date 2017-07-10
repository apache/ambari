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
 * Service responsible for setting resource requests.
 */
@Path("/settings/")
public class SettingService extends BaseService {

  /**
   * Construct a SettingService.
   */
  public SettingService() {

  }

  /**
   * Handles: GET  /settings
   * Get all clusters.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return setting collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createSettingResource(null));
  }

  /**
   * Handles: GET /settings/{settingName}
   * Get a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  settingName
   *
   * @return setting instance representation
   */
  @GET
  @Path("{settingName}")
  @Produces("text/plain")
  public Response getSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("settingName") String settingName) {
    return handleRequest(headers, body, ui, Request.Type.GET, createSettingResource(settingName));
  }

  /**
   * Handles: POST /settings/{settingName}
   * Create a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   *
   * @return information regarding the created setting
   */
   @POST
   @Produces("text/plain")
   public Response createSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
     return handleRequest(headers, body, ui, Request.Type.POST, createSettingResource(null));
  }

  /**
   * Handles: PUT /settings/{settingName}
   * Update a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  setting name
   *
   * @return information regarding the updated setting
   */
  @PUT
  @Path("{settingName}")
  @Produces("text/plain")
  public Response updateSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("settingName") String settingName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createSettingResource(settingName));
  }

  /**
   * Handles: DELETE /settings/{settingName}
   * Delete a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  setting name
   *
   * @return information regarding the deleted setting
   */
  @DELETE
  @Path("{settingName}")
  @Produces("text/plain")
  public Response deleteSetting(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("settingName") String settingName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createSettingResource(settingName));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a setting resource instance.
   *
   * @param settingName setting name
   *
   * @return a setting resource instance
   */
  ResourceInstance createSettingResource(String settingName) {
    return createResource(Resource.Type.Setting,
        Collections.singletonMap(Resource.Type.Setting, settingName));
  }
}