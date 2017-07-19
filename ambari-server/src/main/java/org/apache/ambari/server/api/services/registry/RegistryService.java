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

import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.apache.ambari.server.controller.RegistryResponse.RegistryResponseWrapper;
import org.apache.ambari.server.controller.internal.RegistryResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API endpoint for software registry
 */
@Path("/registries/")
@Api(value = "Registries", description = "Endpoint for software registry specific operations")
public class RegistryService extends BaseService {

  private static final String REGISTRY_REQUEST_TYPE = "org.apache.ambari.server.api.services.registry.RegistryRequestSwagger";

  /**
   * Constructor
   */
  public RegistryService() {
    super();
  }

  /**
   * Handles: POST /registries/
   *
   * @param headers http headers
   * @param ui      uri info
   * @param body    request body
   * @return        information regarding the software registry created
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Connect a software registry with this Ambari instance")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = REGISTRY_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createRegistries(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createRegistryResource(null));
  }

  /**
   * Handles: GET /registries/
   *
   * @param headers http headers
   * @param ui      uri info
   * @param body    request body
   * @return        {@link Response} containing all software registries
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all software registry connected with this Ambari instance",
    response = RegistryResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY, defaultValue = RegistryResourceProvider.REGISTRY_ID),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE,
      dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES,
      defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES,
      dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  public Response getRegistries(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRegistryResource(null));
  }

  /***
   * Handles: GET /registries/{registryId}
   * Return a specific software registry given an registryId
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param body        request body
   * @param registryId  registry id
   * @return            {@link Response} containing information about specific software registry.
   */
  @GET
  @Path("{registryId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about a specific software registry that is connected with this Ambari instance",
    response = RegistryResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING,
      paramType = PARAM_TYPE_QUERY, defaultValue = RegistryResourceProvider.ALL_PROPERTIES),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRegistry(
    String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @PathParam("registryId") String registryId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createRegistryResource(registryId));
  }

  /**
   * Create an software registry resource instance
   *
   * @param registryId registry id
   * @return ResourceInstance
   */
  private ResourceInstance createRegistryResource(String registryId) {
    return createResource(Resource.Type.Registry,
      Collections.singletonMap(Resource.Type.Registry, registryId));

  }
}
