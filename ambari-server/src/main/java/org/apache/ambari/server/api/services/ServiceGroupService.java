/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * Service responsible for servicegroups resource requests.
 */
@Api(value = "Service Groups", description = "Endpoint for servicegroup specific operations")
public class ServiceGroupService extends BaseService {
  private static final String SERVICE_GROUP_REQUEST_TYPE = "org.apache.ambari.server.controller.ServiceGroupRequestSwagger";
  private static final String SERVICE_GROUP_DEPENDENCY_REQUEST_TYPE = "org.apache.ambari.server.controller.ServiceGroupDependencyRequestSwagger";


  /**
   * Parent cluster Name.
   */
  private String m_clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster Name
   */
  public ServiceGroupService(String clusterName) {
    super();
    m_clusterName = clusterName;
  }

  /**
   * Handles: POST /clusters/{clusterName}/servicegroups
   * Create multiple servicegroups.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created servicegroups
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a servicegroup",
    nickname = "ServiceGroupService#createServiceGroups"
  )
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = SERVICE_GROUP_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createServiceGroups(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
      createServiceGroupResource(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterName}/servicegroups
   * Get all servicegroups for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all servicegroups",
    nickname = "ServiceGroupService#getServiceGroups",
    notes = "Returns all servicegroups.",
    response = ServiceGroupResponse.ServiceGroupResponseSwagger.class,
    responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
      defaultValue = "ServiceGroupInfo/service_group_name, ServiceGroupInfo/cluster_name",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
      defaultValue = "ServiceGroupInfo/service_group_name.asc, ServiceGroupInfo/cluster_name.asc",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceGroups(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createServiceGroupResource(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterName}/servicegroups/{serviceGroupName}
   * Get a specific servicegroup.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param serviceGroupName  service group name
   * @return servicegroup    resource representation
   */
  @GET
  @Path("{serviceGroupName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a servicegroup",
    nickname = "ServiceGroupService#getServiceGroup",
    notes = "Returns the details of a servicegroup",
    response = ServiceGroupResponse.ServiceGroupResponseSwagger.class,
    responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ServiceGroupInfo/*",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                  @PathParam("serviceGroupName") String serviceGroupName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createServiceGroupResource(m_clusterName, serviceGroupName));
  }

  /**
   * Handles: PUT /clusters/{clusterName}/servicegroups
   * Update multiple servicegroups.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the updated servicegroups
   */
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates multiple servicegroups",
    nickname = "ServiceGroupService#updateServiceGroups"
  )
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = SERVICE_GROUP_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateServiceGroups(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceGroupResource(m_clusterName, null));
  }

  /**
   * Handles: PUT /clusters/{clusterName}/servicegroups/{serviceGroupName}
   * Update a specific servicegroup.
   *
   * @param body                http body
   * @param headers             http headers
   * @param ui                  uri info
   * @param serviceGroupName    service group name
   * @return information regarding the updated servicegroup
   */
  @PUT
  @Path("{serviceGroupName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a servicegroup",
    nickname = "ServiceGroupService#updateServiceGroup"
  )
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = SERVICE_GROUP_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateServiceGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                     @PathParam("serviceGroupName") String serviceGroupName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceGroupResource(m_clusterName, serviceGroupName));
  }

  /**
   * Handles: DELETE /clusters/{clusterName}/servicegroups/{serviceGroupName}
   * Delete a specific servicegroup.

   * @param headers           http headers
   * @param ui                uri info
   * @param serviceGroupName  service group name
   * @return information regarding the deleted servicegroup
   */
  @DELETE
  @Path("{serviceGroupName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a servicegroup",
    nickname = "ServiceGroupService#deleteServiceGroup"
  )
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteServiceGroup(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("serviceGroupName") String serviceGroupName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createServiceGroupResource(m_clusterName, serviceGroupName));
  }

  /**
   * Get the services sub-resource
   *
   * @param request           the request
   * @param serviceGroupName  service group Name
   *
   * @return the services service
   */
  @Path("{serviceGroupName}/services")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public ServiceService getServiceHandler(@Context javax.ws.rs.core.Request request, @PathParam("serviceGroupName") String serviceGroupName) {
    return new ServiceService(m_clusterName, serviceGroupName);
  }

  /**
   * Handles URL: /clusters/{clusterName}/servicegroups/{serviceGroupName}/dependencies
   * Get all servicegroupdependencies for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service group dependencies collection resource representation
   */
  @GET
  @Path("{serviceGroupName}/dependencies")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all servicegroupdependencies",
          nickname = "ServiceGroupService#getServiceGroupDependencies",
          notes = "Returns all servicegroupdependencies.",
          response = ServiceGroupDependencyResponse.ServiceGroupDependencyResponseSwagger.class,
          responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
                  defaultValue = "ServiceGroupDependencyInfo/dependency_service_group_id, ServiceGroupInfo/service_group_name, ServiceGroupInfo/cluster_name",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
                  defaultValue = "ServiceGroupDependencyInfo/dependency_service_group_id.asc, ServiceGroupInfo/service_group_name.asc, ServiceGroupInfo/cluster_name.asc",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceGroupDependencies(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                  @PathParam("serviceGroupName") String serviceGroupName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createServiceGroupDependencyResource(m_clusterName, serviceGroupName, null));
  }

  /**
   * Handles URL: /clusters/{clusterName}/servicegroups/{serviceGroupName}/dependencies/{serviceGroupDependency}
   * Get a specific servicegroupdependency.
   *
   * @param headers                    http headers
   * @param ui                         uri info
   * @param serviceGroupName           service group name
   * @param serviceGroupDependencyId   service group dependency id
   * @return servicegroupdependency    resource representation
   */
  @GET
  @Path("{serviceGroupName}/dependencies/{serviceGroupDependencyId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a servicegroupdependency",
          nickname = "ServiceGroupService#getServiceGroupDependency",
          notes = "Returns the details of a servicegroupdependency",
          response = ServiceGroupResponse.ServiceGroupResponseSwagger.class,
          responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ServiceGroupInfo/*",
                  dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceGroupDependency(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                              @PathParam("serviceGroupName") String serviceGroupName,
                                              @PathParam("serviceGroupDependencyId") String serviceGroupDependencyId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createServiceGroupDependencyResource(m_clusterName, serviceGroupName, serviceGroupDependencyId));
  }

  /**
   * Handles: POST /clusters/{clusterName}/servicegroups/{serviceGroupName}/dependencies
   * Create multiple servicegroupdependencies.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created servicegroupdependencies
   */
  @POST
  @Path("{serviceGroupName}/dependencies")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a servicegroupdependency",
          nickname = "ServiceGroupService#addServiceGroupDependency"
  )
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = SERVICE_GROUP_DEPENDENCY_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response addServiceGroupDependency(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                              @PathParam("serviceGroupName") String serviceGroupName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
            createServiceGroupDependencyResource(m_clusterName, serviceGroupName, null));
  }

  /**
   * Handles: DELETE /clusters/{clusterName}/servicegroups/{serviceGroupName}/dependencies/{serviceGroupDependency}
   * Delete a specific servicegroupdependency.

   * @param headers                   http headers
   * @param ui                        uri info
   * @param serviceGroupName          service group name
   * @param serviceGroupDependencyId  service group dependency id
   * @return information regarding the deleted servicegroupdependency
   */
  @DELETE
  @Path("{serviceGroupName}/dependencies/{serviceGroupDependencyId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a servicegroupdependency",
          nickname = "ServiceGroupService#deleteServiceGroupDependency"
  )
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteServiceGroupDependency(@Context HttpHeaders headers, @Context UriInfo ui,
                                     @PathParam("serviceGroupName") String serviceGroupName,
                                     @PathParam("serviceGroupDependencyId") String serviceGroupDependencyId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createServiceGroupDependencyResource(m_clusterName, serviceGroupName, serviceGroupDependencyId));
  }
  /**
   * Create a service resource instance.
   *
   * @param clusterName  cluster Name
   * @param serviceGroupName  servicegroup Name
   *
   * @return a service resource instance
   */
  ResourceInstance createServiceGroupResource(String clusterName, String serviceGroupName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ServiceGroup, serviceGroupName);


    return createResource(Resource.Type.ServiceGroup, mapIds);
  }

  ResourceInstance createServiceGroupDependencyResource(String clusterName, String serviceGroupName, String serviceGroupDependencyId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ServiceGroup, serviceGroupName);
    mapIds.put(Resource.Type.ServiceGroupDependency, serviceGroupDependencyId);


    return createResource(Resource.Type.ServiceGroupDependency, mapIds);
  }
}
