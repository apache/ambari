/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.settings;

import org.apache.ambari.view.hive20.BaseService;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.utils.NotFoundFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Service to support the API call for basic CRUD operations of User Setting
 */
public class SettingsService extends BaseService {

  protected final Logger LOG =
      LoggerFactory.getLogger(getClass());

  private final SettingsResourceManager resourceManager;

  @Inject
  public SettingsService(SettingsResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }


  /**
   * Gets all the settings for the current user
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll() {
    List<Setting> settings = resourceManager.getSettings();
    JSONObject response = new JSONObject();
    response.put("settings", settings);
    return Response.ok(response).build();
  }

  /**
   * Adds a setting for the current user
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addSetting(SettingRequest settingRequest, @Context HttpServletResponse response, @Context UriInfo uriInfo) {
    Setting setting = resourceManager.create(settingRequest.getSetting());

    response.setHeader("Location",
        String.format("%s/%s", uriInfo.getAbsolutePath().toString(), setting.getId()));

    JSONObject op = new JSONObject();
    op.put("setting", setting);
    return Response.ok(op).build();
  }

  /**
   * Updates a setting for the current user
   */
  @PUT
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateSetting(@PathParam("id") String id, SettingRequest settingRequest, @Context HttpServletResponse response, @Context UriInfo uriInfo) {
    Setting setting = null;
    try {
      setting = resourceManager.update(id, settingRequest.getSetting());
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Error occurred while creating settings : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    }

    response.setHeader("Location",
        String.format("%s/%s", uriInfo.getAbsolutePath().toString(), setting.getId()));

    JSONObject op = new JSONObject();
    op.put("setting", setting);
    return Response.ok(op).build();
  }

  /**
   * Deletes a setting for the current user
   */
  @DELETE
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@PathParam("id") String id) {
    try {
      resourceManager.removeSetting(id);
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Error occurred while updating setting : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    }
    return Response.noContent().build();
  }

  /**
   * Wrapper class for settings request
   */
  public static class SettingRequest {
    private Setting setting;

    public Setting getSetting() {
      return setting;
    }

    public void setSetting(Setting setting) {
      this.setting = setting;
    }
  }
}
