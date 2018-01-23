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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ReadOnlyConfigurationResponse;
import org.apache.ambari.server.controller.internal.MpackResourceProvider;
import org.apache.ambari.server.controller.internal.RootClusterSettingsResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for 'root cluster settings' resource requests.
 */
@Path("/cluster_settings/")
@Api(value = "ClusterSettings", description = "Endpoint for fetching read only 'cluster settings'")
public class RootClusterSettingService extends BaseService {

  public RootClusterSettingService() {
    super();
  }

  /**
   * Handles: GET /cluster_settings
   * Get all default settings for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information for all the read only 'cluster settings'",
          response = ReadOnlyConfigurationResponse.ReadOnlyConfigurationResponseSwagger.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
                  paramType = PARAM_TYPE_QUERY, defaultValue = MpackResourceProvider.MPACK_ID),
          @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING,
                  paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE,
                  dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES,
                  defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES,
                  dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })

  public Response getClusterSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createClusterSettingsResource(null));
  }

  /**
   * Handles GET /cluster_settings/{settingName}
   * Get specific default setting for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Path("{settingName}")
  @Produces("text/plain")
  @ApiOperation(value = "Returns information about specific read only 'cluster settings'",
          response = ReadOnlyConfigurationResponse.ReadOnlyConfigurationResponseSwagger.class)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
                  paramType = PARAM_TYPE_QUERY, defaultValue = RootClusterSettingsResourceProvider.ALL_PROPERTIES),
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getClusterSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                    @PathParam("settingName") String settingName) {
    return handleRequest(headers, body, ui, Request.Type.GET, createClusterSettingsResource(settingName));
  }

  /**
   * Create a 'cluster setting' resource instance.
   *
   * @return 'cluster setting' resource instance
   */
  ResourceInstance createClusterSettingsResource(String settingName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootClusterSetting, settingName);

    return createResource(Resource.Type.RootClusterSetting, mapIds);
  }
}
