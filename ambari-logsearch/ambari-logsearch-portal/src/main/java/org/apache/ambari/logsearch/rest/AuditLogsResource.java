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
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.model.request.impl.AnyGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.AuditBarGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.BaseAuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.FieldAuditBarGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.FieldAuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.SimpleQueryRequest;
import org.apache.ambari.logsearch.model.request.impl.UserExportRequest;
import org.apache.ambari.logsearch.model.response.AuditLogResponse;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.query.model.AnyGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.AuditLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.AuditBarGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.ambari.logsearch.query.model.FieldAuditLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.FieldAuditBarGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.SearchCriteria;
import org.apache.ambari.logsearch.model.request.impl.AuditLogRequest;
import org.apache.ambari.logsearch.manager.AuditLogsManager;
import org.apache.ambari.logsearch.query.model.UserExportSearchCriteria;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.AuditOperationDescriptions.*;

@Api(value = "audit/logs", description = "Audit log operations")
@Path("audit/logs")
@Component
@Scope("request")
public class AuditLogsResource {

  @Inject
  private AuditLogsManager auditLogsManager;

  @Inject
  private ConversionService conversionService;

  @GET
  @Path("/schema/fields")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_SCHEMA_FIELD_LIST_OD)
  public String getSolrFieldList() {
    return auditLogsManager.getAuditLogsSchemaFieldsName();
  }

  @GET
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LOGS_OD)
  public AuditLogResponse getAuditLogs(@BeanParam AuditLogRequest auditLogRequest) {
    return auditLogsManager.getLogs(conversionService.convert(auditLogRequest, AuditLogSearchCriteria.class));
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_COMPONENTS_OD)
  public GroupListResponse getAuditComponents(@BeanParam SimpleQueryRequest request) {
    return auditLogsManager.getAuditComponents(conversionService.convert(request, CommonSearchCriteria.class));
  }

  @GET
  @Path("/bargraph")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LINE_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAuditBarGraphData(@BeanParam AuditBarGraphRequest request) {
    return auditLogsManager.getAuditBarGraphData(conversionService.convert(request, AuditBarGraphSearchCriteria.class));
  }

  @GET
  @Path("/users")
  @Produces({"application/json"})
  @ApiOperation(GET_TOP_AUDIT_USERS_OD)
  public BarGraphDataListResponse getTopAuditUsers(@BeanParam FieldAuditBarGraphRequest request) {
    return auditLogsManager.topTenUsers(conversionService.convert(request, FieldAuditBarGraphSearchCriteria.class));
  }

  @GET
  @Path("/resources")
  @Produces({"application/json"})
  @ApiOperation(GET_TOP_AUDIT_RESOURCES_OD)
  public BarGraphDataListResponse getTopAuditResources(@BeanParam FieldAuditLogRequest request) {
    return auditLogsManager.topTenResources(conversionService.convert(request, FieldAuditLogSearchCriteria.class));
  }

  @GET
  @Path("/live/count")
  @Produces({"application/json"})
  @ApiOperation(GET_LIVE_LOGS_COUNT_OD)
  public NameValueDataListResponse getLiveLogsCount() {
    return auditLogsManager.getLiveLogCounts();
  }

  @GET
  @Path("/request/user/bargraph")
  @Produces({"application/json"})
  @ApiOperation(GET_REQUEST_USER_LINE_GRAPH_OD)
  public BarGraphDataListResponse getRequestUserBarGraph(@BeanParam FieldAuditBarGraphRequest request) {
    return auditLogsManager.getRequestUserLineGraph(conversionService.convert(request, FieldAuditBarGraphSearchCriteria.class));
  }

  @GET
  @Path("/anygraph")
  @Produces({"application/json"})
  @ApiOperation(GET_ANY_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAnyGraphData(@BeanParam AnyGraphRequest request) {
    return auditLogsManager.getAnyGraphData(conversionService.convert(request, AnyGraphSearchCriteria.class));
  }

  @GET
  @Path("/users/export")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_USER_TALBE_TO_TEXT_FILE_OD)
  public Response exportUserTableToTextFile(@BeanParam UserExportRequest request) {
    return auditLogsManager.exportUserTableToTextFile(conversionService.convert(request, UserExportSearchCriteria.class));
  }

  @GET
  @Path("/serviceload")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOAD_OD)
  public BarGraphDataListResponse getServiceLoad(@BeanParam BaseAuditLogRequest request) {
    return auditLogsManager.getServiceLoad(conversionService.convert(request, CommonSearchCriteria.class));
  }

}
