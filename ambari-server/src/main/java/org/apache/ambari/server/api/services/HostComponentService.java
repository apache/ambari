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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for host_components resource requests.
 */
@Api(value = "HostComponents", description = "Endpoint for host component specific operations")
public class HostComponentService extends BaseService {
  private static final String HOST_COMPONENT_REQUEST_TYPE = "org.apache.ambari.server.controller.ServiceComponentHostRequestSwagger";


  /**
   * Parent cluster id.
   */
  private String m_clusterName;

  /**
   * Parent host id.
   */
  private String m_hostName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   * @param hostName    host id
   */
  public HostComponentService(String clusterName, String hostName) {
    m_clusterName = clusterName;
    m_hostName = hostName;
  }

  /**
   * Handles GET /clusters/{clusterName}/hosts/{hostID}/host_components/{hostComponentID}
   * Get a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentId   host_component id
   * @return host_component   resource representation
   */
  @GET
  @Path("{hostComponentId}")
  @Produces("text/plain")
  @ApiOperation(value = "Get the details of a given Host Component",
          nickname = "HostComponentService#getHostComponent",
          notes = "Returns the details of a hostComponent",
          response = ServiceComponentHostResponse.ServiceComponentHostResponseSwagger.class,
          responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "HostRoles/*",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentId") String hostComponentId, @QueryParam("format") String format) {

    //todo: needs to be refactored when properly handling exceptions
    if (m_hostName == null) {
      // don't allow case where host is not in url but a host_component instance resource is requested
      String s = "Invalid request. Must provide host information when requesting a host_resource instance resource.";
      return Response.status(400).entity(s).build();
    }

    if (format != null && format.equals("client_config_tar")) {
      return createClientConfigResource(body, headers, ui, hostComponentId);
    }

    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentId));
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID}/host_components/
   * Get all host components for a host.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host_component collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all Host Components",
          nickname = "HostComponentService#getHostComponents",
          notes = "Returns all Host Components.",
          response = ServiceComponentHostResponse.ServiceComponentHostResponseSwagger.class,
          responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
                  defaultValue = "HostRoles/cluster_name, HostRoles/component_name, HostRoles/host_name, HostRoles/id, " +
                          "HostRoles/service_group_name, HostRoles/service_name",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
                  defaultValue = "HostRoles/cluster_name.asc, HostRoles/component_name.asc, HostRoles/host_name.asc, " +
                          "HostRoles/service_group_name.asc, HostRoles/service_name.asc",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui, @QueryParam("format") String format) {
    if (format != null && format.equals("client_config_tar")) {
      return createClientConfigResource(body, headers, ui, null);
    }
    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}/host_components
   * Create host components by specifying an array of host components in the http body.
   * This is used to create multiple host components in a single request.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return status code only, 201 if successful
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a Host Component",
          nickname = "HostComponentService#createHostComponents"
  )
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = HOST_COMPONENT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
          @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Updates a specific host_component.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentId   host_component id
   *
   * @return information regarding updated host_component
   */
  @PUT
  @Path("{hostComponentId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a given Host Component",
          nickname = "HostComponentService#updateHostComponent"
  )
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = HOST_COMPONENT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                      @PathParam("hostComponentId") String hostComponentId) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentId));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components
   * Updates multiple host_component resources.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return information regarding updated host_component resources
   */
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates multiple Host Components",
          nickname = "HostComponentService#updateHostComponents"
  )
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = HOST_COMPONENT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Delete a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentId   host_component id
   *
   * @return host_component resource representation
   */
  @DELETE @ApiIgnore // until documented
  @Path("{hostComponentId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a hostComponent",
          nickname = "HostComponentService#deleteHostComponent"
  )
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHostComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentId") String hostComponentId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentId));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}/host_components
   * Deletes multiple host_component resources.
   *
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return host_component resource representation
   */
  @DELETE @ApiIgnore // until documented
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes multiple hostComponents",
          nickname = "HostComponentService#deleteHostComponents"
  )
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{hostComponentId}/processes")
  @ApiOperation(value = "Get details of processes.",
          nickname = "HostComponentService#getProcesses",
          notes = "Returns the details of a host component processes.",
          responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "HostRoles/*",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getProcesses(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("hostComponentId") String hostComponentId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Host, m_hostName);
    mapIds.put(Resource.Type.HostComponent, hostComponentId);

    ResourceInstance ri = createResource(Resource.Type.HostComponentProcess, mapIds);

    return handleRequest(headers, null, ui, Request.Type.GET, ri);
  }

  /**
   * Create a host_component resource instance.
   *
   * @param clusterName       cluster name
   * @param hostName          host name
   * @param hostComponentId   host_component id
   *
   * @return a host resource instance
   */
  ResourceInstance createHostComponentResource(String clusterName, String hostName, String hostComponentId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.HostComponent, hostComponentId);

    return createResource(Resource.Type.HostComponent, mapIds);
  }

  private Response createClientConfigResource(String body, HttpHeaders headers, UriInfo ui,
                                              String hostComponentId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Host, m_hostName);
    mapIds.put(Resource.Type.Component, hostComponentId);

    Response response = handleRequest(headers, body, ui, Request.Type.GET,
            createResource(Resource.Type.ClientConfig, mapIds));

    //If response has errors return response
    if (response.getStatus() != 200) {
      return response;
    }

    String filePrefixName;

    if (StringUtils.isEmpty(hostComponentId)) {
      filePrefixName = m_hostName + "(" + Resource.InternalType.Host.toString().toUpperCase()+")";
    } else {
      filePrefixName = hostComponentId;
    }

    Validate.notNull(filePrefixName, "compressed config file name should not be null");
    String fileName =  filePrefixName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION;

    Response.ResponseBuilder rb = Response.status(Response.Status.OK);
    Configuration configs = new Configuration();
    String tmpDir = configs.getProperty(Configuration.SERVER_TMP_DIR.getKey());
    File file = new File(tmpDir,fileName);
    InputStream resultInputStream = null;
    try {
      resultInputStream = new FileInputStream(file);
    } catch (IOException e) {
      e.printStackTrace();
    }

    String contentType = Configuration.DEF_ARCHIVE_CONTENT_TYPE;
    rb.header("Content-Disposition",  "attachment; filename=\"" + fileName + "\"");
    rb.entity(resultInputStream);
    return rb.type(contentType).build();

  }

}
