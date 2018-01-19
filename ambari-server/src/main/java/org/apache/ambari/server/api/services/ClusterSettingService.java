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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ClusterSettingResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;


import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for services resource requests.
 */
public class ClusterSettingService extends BaseService {
    private static final String CLUSTER_SETTING_REQUEST_TYPE = "org.apache.ambari.server.controller.ClusterSettingRequestSwagger";

    /**
     * Parent cluster name.
     */
    private String m_clusterName;

    /**
     * Constructor.
     *
     * @param clusterName cluster id
     */
    public ClusterSettingService(String clusterName) {
        m_clusterName = clusterName;
    }


    /**
     * Handles URL: /clusters/{clusterId}/settings
     * Get all 'cluster settings' for a cluster.
     *
     * @param headers http headers
     * @param ui      uri info
     * @return service collection resource representation
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get all cluster settings",
            nickname = "ClusterSettingService#getClusterSettings",
            notes = "Returns all 'cluster settings'.",
            response = ClusterSettingResponse.ClusterSettingResponseSwagger.class,
            responseContainer = RESPONSE_CONTAINER_LIST)
    @ApiImplicitParams({
            @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
                    defaultValue = "ClusterSettingInfo/cluster_setting_name, ClusterSettingInfo/cluster_name",
                    dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
                    defaultValue = "ClusterSettingInfo/cluster_setting_name.asc, ClusterSettingInfo/cluster_name.asc",
                    dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
    })
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
    })
    public Response getClusterSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
        return handleRequest(headers, body, ui, Request.Type.GET,
                createClusterSettingResource(m_clusterName, null));
    }

    /**
     * Handles URL: /clusters/{clusterId}/settings/{clusterSettingName}
     * Get a specific 'cluster setting' for a cluster.
     *
     * @param headers http headers
     * @param ui      uri info
     * @return service collection resource representation
     */
    @GET
    @Path("{clusterSettingName}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get the details of a specific 'cluster setting'",
            nickname = "ClusterSettingService#getClusterSetting",
            notes = "Returns the details of a specific 'cluster setting'",
            response = ClusterSettingResponse.ClusterSettingResponseSwagger.class,
            responseContainer = RESPONSE_CONTAINER_LIST)
    @ApiImplicitParams({
            @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ClusterSettingInfo/*",
                    dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
    })
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
    })
    public Response getClusterSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                      @PathParam("clusterSettingName") String clusterSettingName) {
        return handleRequest(headers, body, ui, Request.Type.GET,
                createClusterSettingResource(m_clusterName, clusterSettingName));
    }

    /**
     * Handles: PUT /clusters/{clusterName}/settings
     * Update multiple cluster settings.
     *
     * @param body        http body
     * @param headers     http headers
     * @param ui          uri info
     * @return information regarding the updated cluster settings.
     */
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Updates multiple cluster settings",
            nickname = "ClusterSettingService#updateClusterSettings"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(dataType = CLUSTER_SETTING_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
    public Response updateClusterSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

        return handleRequest(headers, body, ui, Request.Type.PUT, createClusterSettingResource(m_clusterName, null));
    }

    /**
     * Handles: PUT /clusters/{clusterName}/settings/{clusterSettingName}
     * Updates the specific cluster setting.
     *
     * @param body                  http body
     * @param headers               http headers
     * @param ui                    uri info
     * @param clusterSettingName    cluster setting name
     *
     * @return information regarding the updated cluster setting.
     */
    @PUT
    @Path("{clusterSettingName}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Updates the specific cluster setting",
            nickname = "ClusterSettingService#updateClusterSetting"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(dataType = CLUSTER_SETTING_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
    public Response updateClusterSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                          @PathParam("clusterSettingName") String clusterSettingName) {
        return handleRequest(headers, body, ui, Request.Type.PUT, createClusterSettingResource(m_clusterName, clusterSettingName));
    }

    /**
     * Handles: DELETE /clusters/{clusterName}/settings/{clusterSettingName}
     * Delete a specific 'cluster setting'.

     * @param headers               http headers
     * @param ui                    uri info
     * @param clusterSettingName    cluster setting name
     * @return information regarding the deleted 'cluster setting'
     */
    @DELETE
    @Path("{clusterSettingName}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Deletes a specific cluster setting",
            nickname = "ClusterSettingService#deleteClustersetting"
    )
    @ApiResponses({
            @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    })
    public Response deleteClusterSetting(@Context HttpHeaders headers, @Context UriInfo ui,
                                       @PathParam("clusterSettingName") String clusterSettingName) {

        return handleRequest(headers, null, ui, Request.Type.DELETE, createClusterSettingResource(m_clusterName, clusterSettingName));
    }

    /**
     * Handles URL: /clusters/{clusterId}/settings
     * The body should contain:
     * To create multiple settings in a request, provide an array of settings.
     * eg:
     *
     * [
     *      {
     *          "ClusterSettingInfo" : {
     *              "cluster_setting_name": "{setting_name}",
     *              "cluster_setting_value": "{setting_val}"
     *          }
     *      },
     *      {
     *          "ClusterSettingInfo" : {
     *              "cluster_setting_name": "{setting_name}",
     *              "cluster_setting_value": "{setting_val}"
     *      }
     *]
     *
     *
     *
     * @param headers http headers
     * @param ui      uri info
     * @return status code only, 201 if successful
     */
    @POST @ApiIgnore // until documented
    @Produces("text/plain")
    public Response createConfigurations(String body,@Context HttpHeaders headers, @Context UriInfo ui) {
        return handleRequest(headers, body, ui, Request.Type.POST, createClusterSettingResource(m_clusterName, null));
    }

    /**
     * Create a 'cluster setting' resource instance.
     *
     * @param clusterName cluster name
     *
     * @return a cluster setting resource instance
     */
    ResourceInstance createClusterSettingResource(String clusterName, String clusterSettingName) {
        Map<Resource.Type,String> mapIds = new HashMap<>();
        mapIds.put(Resource.Type.Cluster, clusterName);
        mapIds.put(Resource.Type.ClusterSetting, clusterSettingName);

        return createResource(Resource.Type.ClusterSetting, mapIds);
    }
}