/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.rest;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.model.metadata.FieldMetadata;
import org.apache.ambari.logsearch.model.metadata.ServiceComponentMetadataWrapper;
import org.apache.ambari.logsearch.model.request.impl.body.ClusterBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.HostLogFilesBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceAnyGraphBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceGraphBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogAggregatedInfoBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogComponentHostBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogComponentLevelBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogExportBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogHostComponentBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogLevelCountBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.body.ServiceLogTruncatedBodyRequest;
import org.apache.ambari.logsearch.model.request.impl.query.HostLogFilesQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceAnyGraphQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceGraphQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogAggregatedInfoQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogComponentHostQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogComponentLevelQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogExportQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogHostComponentQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogLevelCountQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.query.ServiceLogTruncatedQueryRequest;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.CountDataListResponse;
import org.apache.ambari.logsearch.model.response.GraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.HostLogFilesResponse;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.model.response.NodeListResponse;
import org.apache.ambari.logsearch.model.response.ServiceLogResponse;
import org.apache.ambari.logsearch.manager.ServiceLogsManager;
import org.springframework.context.annotation.Scope;

import java.util.List;

import static org.apache.ambari.logsearch.doc.DocConstants.ServiceOperationDescriptions.*;

@Api(value = "service/logs", description = "Service log operations")
@Path("service/logs")
@Named
@Scope("request")
public class ServiceLogsResource {

  @Inject
  private ServiceLogsManager serviceLogsManager;

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(SEARCH_LOGS_OD)
  public ServiceLogResponse searchServiceLogsGet(@BeanParam ServiceLogQueryRequest request) {
    return serviceLogsManager.searchLogs(request);
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(SEARCH_LOGS_OD)
  public ServiceLogResponse searchServiceLogsPost(ServiceLogBodyRequest request) {
    return serviceLogsManager.searchLogs(request);
  }

  @DELETE
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(PURGE_LOGS_OD)
  public StatusMessage deleteServiceLogs(ServiceLogBodyRequest request) {
    return serviceLogsManager.deleteLogs(request);
  }

  @GET
  @Path("/hosts")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOSTS_OD)
  public GroupListResponse getHostsGet(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getHosts(clusters);
  }

  @POST
  @Path("/hosts")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOSTS_OD)
  public GroupListResponse getHostsPost(@Nullable ClusterBodyRequest clusterBodyRequest) {
    return serviceLogsManager.getHosts(clusterBodyRequest != null ? clusterBodyRequest.getClusters() : null);
  }

  @GET
  @Path("/components")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENTS_OD)
  public ServiceComponentMetadataWrapper getComponents(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getComponentMetadata(clusters);
  }

  @POST
  @Path("/components")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENTS_OD)
  public ServiceComponentMetadataWrapper getComponents(@Nullable ClusterBodyRequest clusterBodyRequest) {
    return serviceLogsManager.getComponentMetadata(clusterBodyRequest != null ? clusterBodyRequest.getClusters() : null);
  }

  @GET
  @Path("/aggregated")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AGGREGATED_INFO_OD)
  public GraphDataListResponse getAggregatedInfoGet(@BeanParam ServiceLogAggregatedInfoQueryRequest request) {
    return serviceLogsManager.getAggregatedInfo(request);
  }

  @POST
  @Path("/aggregated")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AGGREGATED_INFO_OD)
  public GraphDataListResponse getAggregatedInfoPost(ServiceLogAggregatedInfoBodyRequest request) {
    return serviceLogsManager.getAggregatedInfo(request);
  }

  @GET
  @Path("/components/count")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENTS_COUNT_OD)
  public CountDataListResponse getComponentsCountGet(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getComponentsCount(clusters);
  }

  @POST
  @Path("/components/count")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENTS_COUNT_OD)
  public CountDataListResponse getComponentsCountPost(@Nullable ClusterBodyRequest clusterBodyRequest) {
    return serviceLogsManager.getComponentsCount(clusterBodyRequest != null ? clusterBodyRequest.getClusters() : null);
  }

  @GET
  @Path("/hosts/count")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOSTS_COUNT_OD)
  public CountDataListResponse getHostsCountGet(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getHostsCount(clusters);
  }

  @POST
  @Path("/hosts/count")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOSTS_COUNT_OD)
  public CountDataListResponse getHostsCountPost(@Nullable ClusterBodyRequest clusterBodyRequest) {
    return serviceLogsManager.getHostsCount(clusterBodyRequest != null ? clusterBodyRequest.getClusters() : null);
  }

  @GET
  @Path("/tree")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_TREE_EXTENSION_OD)
  public NodeListResponse getTreeExtensionGet(@BeanParam ServiceLogHostComponentQueryRequest request) {
    return serviceLogsManager.getTreeExtension(request);
  }

  @POST
  @Path("/tree")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_TREE_EXTENSION_OD)
  public NodeListResponse getTreeExtensionPost(ServiceLogHostComponentBodyRequest request) {
    return serviceLogsManager.getTreeExtension(request);
  }

  @GET
  @Path("/levels/counts")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  public NameValueDataListResponse getLogsLevelCountGet(@BeanParam ServiceLogLevelCountQueryRequest request) {
    return serviceLogsManager.getLogsLevelCount(request);
  }

  @POST
  @Path("/levels/counts")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  public NameValueDataListResponse getLogsLevelCountPost(ServiceLogLevelCountBodyRequest request) {
    return serviceLogsManager.getLogsLevelCount(request);
  }

  @GET
  @Path("/histogram")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HISTOGRAM_DATA_OD)
  public BarGraphDataListResponse getHistogramDataGet(@BeanParam ServiceGraphQueryRequest request) {
    return serviceLogsManager.getHistogramData(request);
  }

  @POST
  @Path("/histogram")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HISTOGRAM_DATA_OD)
  public BarGraphDataListResponse getHistogramDataPost(ServiceGraphBodyRequest request) {
    return serviceLogsManager.getHistogramData(request);
  }


  @GET
  @Path("/export")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(EXPORT_TO_TEXT_FILE_OD)
  public Response exportToTextFileGet(@BeanParam ServiceLogExportQueryRequest request) {
    return serviceLogsManager.export(request);
  }

  @POST
  @Path("/export")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(EXPORT_TO_TEXT_FILE_OD)
  public Response exportToTextFilePost(ServiceLogExportBodyRequest request) {
    return serviceLogsManager.export(request);
  }

  @GET
  @Path("/hosts/components")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOST_LIST_BY_COMPONENT_OD)
  public NodeListResponse getHostListByComponentGet(@BeanParam ServiceLogComponentHostQueryRequest request) {
    return serviceLogsManager.getHostListByComponent(request);
  }

  @POST
  @Path("/hosts/components")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOST_LIST_BY_COMPONENT_OD)
  public NodeListResponse getHostListByComponentPost(ServiceLogComponentHostBodyRequest request) {
    return serviceLogsManager.getHostListByComponent(request);
  }

  @GET
  @Path("/components/levels/counts")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENT_LIST_WITH_LEVEL_COUNT_OD)
  public NodeListResponse getComponentListWithLevelCountsGet(@BeanParam ServiceLogComponentLevelQueryRequest request) {
    return serviceLogsManager.getComponentListWithLevelCounts(request);
  }

  @POST
  @Path("/components/levels/counts")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_COMPONENT_LIST_WITH_LEVEL_COUNT_OD)
  public NodeListResponse getComponentListWithLevelCountsPost(ServiceLogComponentLevelBodyRequest request) {
    return serviceLogsManager.getComponentListWithLevelCounts(request);
  }

  @GET
  @Path("/schema/fields")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_LOGS_SCHEMA_FIELD_NAME_OD)
  public List<FieldMetadata> getServiceLogsSchemaFieldsNameGet() {
    return serviceLogsManager.getServiceLogsSchemaFieldsName();
  }

  @POST
  @Path("/schema/fields")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_LOGS_SCHEMA_FIELD_NAME_OD)
  public List<FieldMetadata> getServiceLogsSchemaFieldsNamePost() {
    return serviceLogsManager.getServiceLogsSchemaFieldsName();
  }

  @GET
  @Path("/count/anygraph")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_ANY_GRAPH_COUNT_DATA_OD)
  public BarGraphDataListResponse getAnyGraphCountDataGet(@BeanParam ServiceAnyGraphQueryRequest request) {
    return serviceLogsManager.getAnyGraphCountData(request);
  }

  @POST
  @Path("/count/anygraph")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_ANY_GRAPH_COUNT_DATA_OD)
  public BarGraphDataListResponse getAnyGraphCountDataPost(ServiceAnyGraphBodyRequest request) {
    return serviceLogsManager.getAnyGraphCountData(request);
  }

  @GET
  @Path("/truncated")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AFTER_BEFORE_LOGS_OD)
  public ServiceLogResponse getAfterBeforeLogs(@BeanParam ServiceLogTruncatedQueryRequest request) {
    return serviceLogsManager.getAfterBeforeLogs(request);
  }

  @POST
  @Path("/truncated")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_AFTER_BEFORE_LOGS_OD)
  public ServiceLogResponse getAfterBeforeLogs(ServiceLogTruncatedBodyRequest request) {
    return serviceLogsManager.getAfterBeforeLogs(request);
  }

  @GET
  @Path("/request/cancel")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(REQUEST_CANCEL)
  public String cancelRequestGet() {
    // TODO: create function that cancels an ongoing solr request
    return "{\"endpoint status\": \"not supported yet\"}";
  }

  @POST
  @Path("/request/cancel")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(REQUEST_CANCEL)
  public String cancelRequestPost() {
    // TODO: create function that cancels an ongoing solr request
    return "{\"endpoint status\": \"not supported yet\"}";
  }

  @GET
  @Path("/files")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOST_LOGFILES_OD)
  @ValidateOnExecution
  public HostLogFilesResponse getHostLogFiles(@Valid @BeanParam HostLogFilesQueryRequest request) {
    return serviceLogsManager.getHostLogFileData(request);
  }

  @POST
  @Path("/files")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_HOST_LOGFILES_OD)
  @ValidateOnExecution
  public HostLogFilesResponse getHostLogFiles(@Valid @BeanParam HostLogFilesBodyRequest request) {
    return serviceLogsManager.getHostLogFileData(request);
  }

  @GET
  @Path("/clusters")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_CLUSTERS_OD)
  public List<String> getClustersForServiceLogGet() {
    return serviceLogsManager.getClusters();
  }

  @POST
  @Path("/clusters")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(GET_SERVICE_CLUSTERS_OD)
  public List<String> getClustersForServiceLogPost() {
    return serviceLogsManager.getClusters();
  }

}
