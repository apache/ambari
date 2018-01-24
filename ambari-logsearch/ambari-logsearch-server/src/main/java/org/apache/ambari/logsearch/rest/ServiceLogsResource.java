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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.model.metadata.FieldMetadata;
import org.apache.ambari.logsearch.model.metadata.ServiceComponentMetadataWrapper;
import org.apache.ambari.logsearch.model.request.impl.HostLogFilesRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceAnyGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogAggregatedInfoRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogComponentHostRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogComponentLevelRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogExportRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogHostComponentRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogLevelCountRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogTruncatedRequest;
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
  @Produces({"application/json"})
  @ApiOperation(SEARCH_LOGS_OD)
  public ServiceLogResponse searchServiceLogs(@BeanParam ServiceLogRequest request) {
    return serviceLogsManager.searchLogs(request);
  }

  @DELETE
  @Produces({"application/json"})
  @ApiOperation(PURGE_LOGS_OD)
  public StatusMessage deleteServiceLogs(@BeanParam ServiceLogRequest request) {
    return serviceLogsManager.deleteLogs(request);
  }

  @GET
  @Path("/hosts")
  @Produces({"application/json"})
  @ApiOperation(GET_HOSTS_OD)
  public GroupListResponse getHosts(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getHosts(clusters);
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENTS_OD)
  public ServiceComponentMetadataWrapper getComponents(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getComponentMetadata(clusters);
  }

  @GET
  @Path("/aggregated")
  @Produces({"application/json"})
  @ApiOperation(GET_AGGREGATED_INFO_OD)
  public GraphDataListResponse getAggregatedInfo(@BeanParam ServiceLogAggregatedInfoRequest request) {
    return serviceLogsManager.getAggregatedInfo(request);
  }

  @GET
  @Path("/components/count")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENTS_COUNT_OD)
  public CountDataListResponse getComponentsCount(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getComponentsCount(clusters);
  }

  @GET
  @Path("/hosts/count")
  @Produces({"application/json"})
  @ApiOperation(GET_HOSTS_COUNT_OD)
  public CountDataListResponse getHostsCount(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return serviceLogsManager.getHostsCount(clusters);
  }

  @GET
  @Path("/tree")
  @Produces({"application/json"})
  @ApiOperation(GET_TREE_EXTENSION_OD)
  public NodeListResponse getTreeExtension(@BeanParam ServiceLogHostComponentRequest request) {
    return serviceLogsManager.getTreeExtension(request);
  }

  @GET
  @Path("/levels/counts")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  public NameValueDataListResponse getLogsLevelCount(@BeanParam ServiceLogLevelCountRequest request) {
    return serviceLogsManager.getLogsLevelCount(request);
  }

  @GET
  @Path("/histogram")
  @Produces({"application/json"})
  @ApiOperation(GET_HISTOGRAM_DATA_OD)
  public BarGraphDataListResponse getHistogramData(@BeanParam ServiceGraphRequest request) {
    return serviceLogsManager.getHistogramData(request);
  }

  @GET
  @Path("/export")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_TO_TEXT_FILE_OD)
  public Response exportToTextFile(@BeanParam ServiceLogExportRequest request) {
    return serviceLogsManager.export(request);
  }

  @GET
  @Path("/hosts/components")
  @Produces({"application/json"})
  @ApiOperation(GET_HOST_LIST_BY_COMPONENT_OD)
  public NodeListResponse getHostListByComponent(@BeanParam ServiceLogComponentHostRequest request) {
    return serviceLogsManager.getHostListByComponent(request);
  }

  @GET
  @Path("/components/levels/counts")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENT_LIST_WITH_LEVEL_COUNT_OD)
  public NodeListResponse getComponentListWithLevelCounts(@BeanParam ServiceLogComponentLevelRequest request) {
    return serviceLogsManager.getComponentListWithLevelCounts(request);
  }

  @GET
  @Path("/schema/fields")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOGS_SCHEMA_FIELD_NAME_OD)
  public List<FieldMetadata> getServiceLogsSchemaFieldsName() {
    return serviceLogsManager.getServiceLogsSchemaFieldsName();
  }

  @GET
  @Path("/count/anygraph")
  @Produces({"application/json"})
  @ApiOperation(GET_ANY_GRAPH_COUNT_DATA_OD)
  public BarGraphDataListResponse getAnyGraphCountData(@BeanParam ServiceAnyGraphRequest request) {
    return serviceLogsManager.getAnyGraphCountData(request);
  }

  @GET
  @Path("/truncated")
  @Produces({"application/json"})
  @ApiOperation(GET_AFTER_BEFORE_LOGS_OD)
  public ServiceLogResponse getAfterBeforeLogs(@BeanParam ServiceLogTruncatedRequest request) {
    return serviceLogsManager.getAfterBeforeLogs(request);
  }

  @GET
  @Path("/request/cancel")
  @Produces({"application/json"})
  @ApiOperation(REQUEST_CANCEL)
  public String cancelRequest() {
    // TODO: create function that cancels an ongoing solr request
    return "{\"endpoint status\": \"not supported yet\"}";
  }

  @GET
  @Path("/files")
  @Produces({"application/json"})
  @ApiOperation(GET_HOST_LOGFILES_OD)
  @ValidateOnExecution
  public HostLogFilesResponse getHostLogFiles(@Valid @BeanParam HostLogFilesRequest request) {
    return serviceLogsManager.getHostLogFileData(request);
  }

  @GET
  @Path("/clusters")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_CLUSTERS_OD)
  public List<String> getClustersForServiceLog() {
    return serviceLogsManager.getClusters();
  }

}
