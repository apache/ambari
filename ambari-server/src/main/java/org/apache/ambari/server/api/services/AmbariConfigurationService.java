/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Rest endpoint for managing ambari configurations. Supports CRUD operations.
 * Ambari configurations are resources that relate to the ambari server instance even before a cluster is provisioned.
 *
 * Ambari configuration resources may be shared with components and services in the cluster
 * (by recommending them as default values)
 *
 * Eg. LDAP configuration is stored as ambariconfiguration.
 * The request payload has the form:
 *
 * <pre>
 *      {
 *        "AmbariConfiguration": {
 *            "type": "ldap-configuration",
 *            "data": [
 *                {
 *                 "authentication.ldap.primaryUrl": "localhost:33389"
 *                },
 *                {
 *                "authentication.ldap.secondaryUrl": "localhost:333"
 *                 },
 *                 {
 *                 "authentication.ldap.baseDn": "dc=ambari,dc=apache,dc=org"
 *                 }
 *                 // ......
 *             ]
 *         }
 *     }
 * </pre>
 */
@Path("/configurations/")
@Api(value = "Ambari Configurations", description = "Endpoint for Ambari configuration related operations")
public class AmbariConfigurationService extends BaseService {

  private static final String AMBARI_CONFIGURATION_REQUEST_TYPE =
      "org.apache.ambari.server.api.services.AmbariConfigurationRequestSwagger";

  /**
   * Creates an ambari configuration resource.
   *
   * @param body    the payload in json format
   * @param headers http headers
   * @param uri     request uri information
   * @return
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates an ambari configuration resource",
      nickname = "AmbariConfigurationService#createAmbariConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = AMBARI_CONFIGURATION_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.POST, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve all ambari configuration resources",
      nickname = "AmbariConfigurationService#getAmbariConfigurations",
      notes = "Returns all Ambari configurations.",
      response = AmbariConfigurationResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "AmbariConfiguration/data, AmbariConfiguration/id, AmbariConfiguration/type",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "AmbariConfiguration/id",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getAmbariConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
  }

  @GET
  @Path("{configurationId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve the details of an ambari configuration resource",
      nickname = "AmbariConfigurationService#getAmbariConfiguration",
      response = AmbariConfigurationResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "AmbariConfiguration/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                         @PathParam("configurationId") String configurationId) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
  }

  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates ambari configuration resources - Not implemented yet",
    nickname = "AmbariConfigurationService#updateAmbariConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = AMBARI_CONFIGURATION_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateAmbariConfiguration() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @DELETE
  @Path("{configurationId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes an ambari configuration resource",
      nickname = "AmbariConfigurationService#deleteAmbariConfiguration")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                            @PathParam("configurationId") String configurationId) {
    return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
  }

}
