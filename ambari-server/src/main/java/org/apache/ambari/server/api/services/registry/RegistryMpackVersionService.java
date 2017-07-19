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
package org.apache.ambari.server.api.services.registry;

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
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.RegistryMpackVersionResponse.RegistryMpackVersionResponseWrapper;
import org.apache.ambari.server.controller.internal.RegistryMpackVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API endpoint for registry mpack version
 */
@Path("/registries/{registryId}/mpacks/{mpackName}/versions/")
@Api(value = "RegistryMpackVersions", description = "Endpoint for registry mpack versions specific operations")
public class RegistryMpackVersionService extends BaseService {

  private static final String REGISTRY_MPACK_VERSION_REQUEST_TYPE = "org.apache.ambari.server.api.services.registry.RegistryMpackVersionRequestSwagger";

  /**
   * Constructor
   */
  public RegistryMpackVersionService() {
    super();
  }

  /**
   * Handles: GET /registries/{registryId}/mpacks/{mpackName}/versions
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param body        request body
   * @param registryId  registry id
   * @param mpackName   management pack name
   * @return            {@link Response} containing all versions of an mpack in a software registry
   *
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all versions of an mpack in a registry connected with this Ambari instance",
    response = RegistryMpackVersionResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY, defaultValue = RegistryMpackVersionResourceProvider.REGISTRY_ID),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE,
      dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES,
      defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES,
      dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  public Response getRegistryMpacks(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @PathParam("registryId") String registryId,
    @PathParam("mpackName") String mpackName) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRegistryMpackVersionResource(registryId, mpackName, null));
  }


  /***
   * Handles: GET /registries/{registryId}/mpacks/{mpackName}/versions/{mpackVersion}
   * Return information about a specific mpack version in a registry
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param body          request body
   * @param registryId    registry id
   * @param mpackName     management pack name
   * @param mpackVersion  management pack version
   * @return              {@link Response} containing information of the specific mpack version
   */
  @GET
  @Path("{mpackVersion}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about a specific mpack version in a registry connected with this Ambari instance",
    response = RegistryMpackVersionResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY, defaultValue = RegistryMpackVersionResourceProvider.ALL_PROPERTIES),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRegistryMpack(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @PathParam("registryId") String registryId, 
    @PathParam("mpackName") String mpackName, 
    @PathParam("mpackVersion") String mpackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createRegistryMpackVersionResource(registryId, mpackName, mpackVersion));
  }

  /**
   * Create a registry mpack resource instance
   * @param registryId  software registry id
   * @param mpackName   mpack name
   * @return {@link ResourceInstance}
   */
  private ResourceInstance createRegistryMpackVersionResource(String registryId, String mpackName, String mpackVersion) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Registry, registryId);
    mapIds.put(Resource.Type.RegistryMpack, mpackName);
    mapIds.put(Resource.Type.RegistryMpackVersion, mpackVersion);

    return createResource(Resource.Type.RegistryMpackVersion, mapIds);
  }
}
