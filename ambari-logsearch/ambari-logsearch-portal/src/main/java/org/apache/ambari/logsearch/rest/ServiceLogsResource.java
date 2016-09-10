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

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceAnyGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceExtremeDatesRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogExportRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogRequest;
import org.apache.ambari.logsearch.model.request.impl.ServiceLogTruncatedRequest;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.CountDataListResponse;
import org.apache.ambari.logsearch.model.response.GraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.model.response.NodeListResponse;
import org.apache.ambari.logsearch.model.response.ServiceLogResponse;
import org.apache.ambari.logsearch.query.model.CommonServiceLogSearchCriteria;
import org.apache.ambari.logsearch.manager.ServiceLogsManager;
import org.apache.ambari.logsearch.query.model.ServiceAnyGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.ServiceExtremeDatesCriteria;
import org.apache.ambari.logsearch.query.model.ServiceGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.ServiceLogExportSearchCriteria;
import org.apache.ambari.logsearch.query.model.ServiceLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.ServiceLogTruncatedSearchCriteria;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.ServiceOperationDescriptions.*;

@Api(value = "service/logs", description = "Service log operations")
@Path("service/logs")
@Component
@Scope("request")
public class ServiceLogsResource {

  @Inject
  private ServiceLogsManager serviceLogsManager;

  @Inject
  private ConversionService conversionService;

  @GET
  @Produces({"application/json"})
  @ApiOperation(SEARCH_LOGS_OD)
  public ServiceLogResponse searchSolrData(@BeanParam ServiceLogRequest request) {
    return serviceLogsManager.searchLogs(conversionService.convert(request, ServiceLogSearchCriteria.class));
  }

  @GET
  @Path("/hosts")
  @Produces({"application/json"})
  @ApiOperation(GET_HOSTS_OD)
  public GroupListResponse getHosts() {
    return serviceLogsManager.getHosts();
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENTS_OD)
  public GroupListResponse getComponents() {
    return serviceLogsManager.getComponents();
  }

  @GET
  @Path("/aggregated")
  @Produces({"application/json"})
  @ApiOperation(GET_AGGREGATED_INFO_OD)
  public GraphDataListResponse getAggregatedInfo(@BeanParam BaseServiceLogRequest request) {
    return serviceLogsManager.getAggregatedInfo(conversionService.convert(request, CommonServiceLogSearchCriteria.class));
  }

  @GET
  @Path("/levels/count")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  public CountDataListResponse getLogLevelsCount() {
    return serviceLogsManager.getLogLevelCount();
  }

  @GET
  @Path("/components/count")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENTS_COUNT_OD)
  public CountDataListResponse getComponentsCount() {
    return serviceLogsManager.getComponentsCount();
  }

  @GET
  @Path("/hosts/count")
  @Produces({"application/json"})
  @ApiOperation(GET_HOSTS_COUNT_OD)
  public CountDataListResponse getHostsCount() {
    return serviceLogsManager.getHostsCount();
  }

  @GET
  @Path("/tree")
  @Produces({"application/json"})
  @ApiOperation(GET_TREE_EXTENSION_OD)
  public NodeListResponse getTreeExtension(@QueryParam("hostName") @ApiParam String hostName, @BeanParam ServiceLogRequest request) {
    ServiceLogSearchCriteria searchCriteria = conversionService.convert(request, ServiceLogSearchCriteria.class);
    searchCriteria.addParam("hostName", hostName); // TODO: use host_name instead - needs UI change
    return serviceLogsManager.getTreeExtension(searchCriteria);
  }

  @GET
  @Path("/levels/counts/namevalues")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  public NameValueDataListResponse getLogsLevelCount(@BeanParam ServiceLogRequest request) {
    return serviceLogsManager.getLogsLevelCount(conversionService.convert(request, ServiceLogSearchCriteria.class));
  }

  @GET
  @Path("/histogram")
  @Produces({"application/json"})
  @ApiOperation(GET_HISTOGRAM_DATA_OD)
  public BarGraphDataListResponse getHistogramData(@BeanParam ServiceGraphRequest request) {
    return serviceLogsManager.getHistogramData(conversionService.convert(request, ServiceGraphSearchCriteria.class));
  }

  @GET
  @Path("/request/cancel")
  @Produces({"application/json"})
  @ApiOperation(CANCEL_FIND_REQUEST_OD)
  public String cancelFindRequest(@QueryParam("token") @ApiParam String token) {
    return serviceLogsManager.cancelFindRequestByDate(token);
  }

  @GET
  @Path("/export")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_TO_TEXT_FILE_OD)
  public Response exportToTextFile(@BeanParam ServiceLogExportRequest request) {
    return serviceLogsManager.exportToTextFile(conversionService.convert(request, ServiceLogExportSearchCriteria.class));

  }

  @GET
  @Path("/hosts/components")
  @Produces({"application/json"})
  @ApiOperation(GET_HOST_LIST_BY_COMPONENT_OD)
  public NodeListResponse getHostListByComponent(@BeanParam ServiceLogRequest request, @QueryParam("componentName") @ApiParam String componentName) {
    ServiceLogSearchCriteria searchCriteria = conversionService.convert(request, ServiceLogSearchCriteria.class);
    searchCriteria.addParam("componentName", componentName); // TODO: use component_name instead - needs UI change
    return serviceLogsManager.getHostListByComponent(searchCriteria);
  }

  @GET
  @Path("/components/levels/counts")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENT_LIST_WITH_LEVEL_COUNT_OD)
  public NodeListResponse getComponentListWithLevelCounts(@BeanParam ServiceLogRequest request) {
    return serviceLogsManager.getComponentListWithLevelCounts(conversionService.convert(request, ServiceLogSearchCriteria.class));
  }

  @GET
  @Path("/solr/boundarydates")
  @Produces({"application/json"})
  @ApiOperation(GET_EXTREME_DATES_FOR_BUNDLE_ID_OD)
  public NameValueDataListResponse getExtremeDatesForBundelId(@BeanParam ServiceExtremeDatesRequest request) {
    return serviceLogsManager.getExtremeDatesForBundelId(conversionService.convert(request, ServiceExtremeDatesCriteria.class));
  }

  @GET
  @Path("/schema/fields")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOGS_SCHEMA_FIELD_NAME_OD)
  public String getServiceLogsSchemaFieldsName() {
    return serviceLogsManager.getServiceLogsSchemaFieldsName();
  }

  @GET
  @Path("/anygraph")
  @Produces({"application/json"})
  @ApiOperation(GET_ANY_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAnyGraphData(@BeanParam ServiceAnyGraphRequest request) {
    return serviceLogsManager.getAnyGraphData(conversionService.convert(request, ServiceAnyGraphSearchCriteria.class));
  }

  @GET
  @Path("/truncated")
  @Produces({"application/json"})
  @ApiOperation(GET_AFTER_BEFORE_LOGS_OD)
  public ServiceLogResponse getAfterBeforeLogs(@BeanParam ServiceLogTruncatedRequest request) {
    return serviceLogsManager.getAfterBeforeLogs(conversionService.convert(request, ServiceLogTruncatedSearchCriteria.class));
  }

  @GET
  @Path("/serviceconfig")
  @Produces({"application/json"})
  @ApiOperation(GET_HADOOP_SERVICE_CONFIG_JSON_OD)
  public String getHadoopServiceConfigJSON() {
    return serviceLogsManager.getHadoopServiceConfigJSON();
  }
}
