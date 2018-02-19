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
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import freemarker.template.TemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.model.metadata.AuditFieldMetadataResponse;
import org.apache.ambari.logsearch.model.request.impl.AuditBarGraphRequest;
import org.apache.ambari.logsearch.model.request.impl.AuditServiceLoadRequest;
import org.apache.ambari.logsearch.model.request.impl.TopFieldAuditLogRequest;
import org.apache.ambari.logsearch.model.request.impl.UserExportRequest;
import org.apache.ambari.logsearch.model.response.AuditLogResponse;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.request.impl.AuditLogRequest;
import org.apache.ambari.logsearch.manager.AuditLogsManager;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

import static org.apache.ambari.logsearch.doc.DocConstants.AuditOperationDescriptions.*;

@Api(value = "audit/logs", description = "Audit log operations")
@Path("audit/logs")
@Named
@Scope("request")
public class AuditLogsResource {

  @Inject
  private AuditLogsManager auditLogsManager;

  @GET
  @Path("/schema/fields")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_SCHEMA_FIELD_LIST_OD)
  public AuditFieldMetadataResponse getSolrFieldList() {
    return auditLogsManager.getAuditLogSchemaMetadata();
  }

  @GET
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LOGS_OD)
  public AuditLogResponse getAuditLogs(@BeanParam AuditLogRequest auditLogRequest) {
    return auditLogsManager.getLogs(auditLogRequest);
  }

  @DELETE
  @Produces({"application/json"})
  @ApiOperation(PURGE_AUDIT_LOGS_OD)
  public StatusMessage deleteAuditLogs(@BeanParam AuditLogRequest auditLogRequest) {
    return auditLogsManager.deleteLogs(auditLogRequest);
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_COMPONENTS_OD)
  public Map<String, String> getAuditComponents(@QueryParam(LogSearchConstants.REQUEST_PARAM_CLUSTER_NAMES) @Nullable String clusters) {
    return auditLogsManager.getAuditComponents(clusters);
  }

  @GET
  @Path("/bargraph")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LINE_GRAPH_DATA_OD)
  public BarGraphDataListResponse getAuditBarGraphData(@BeanParam AuditBarGraphRequest request) {
    return auditLogsManager.getAuditBarGraphData(request);
  }

  @GET
  @Path("/resources/{top}")
  @Produces({"application/json"})
  @ApiOperation(GET_TOP_AUDIT_RESOURCES_OD)
  public BarGraphDataListResponse getResources(@BeanParam TopFieldAuditLogRequest request) {
    return auditLogsManager.topResources(request);
  }

  @GET
  @Path("/export")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_USER_TALBE_TO_TEXT_FILE_OD)
  public Response exportUserTableToTextFile(@BeanParam UserExportRequest request) throws TemplateException {
    return auditLogsManager.export(request);
  }

  @GET
  @Path("/serviceload")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOAD_OD)
  public BarGraphDataListResponse getServiceLoad(@BeanParam AuditServiceLoadRequest request) {
    return auditLogsManager.getServiceLoad(request);
  }

  @GET
  @Path("/clusters")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_CLUSTERS_OD)
  public List<String> getClustersForAuditLog() {
    return auditLogsManager.getClusters();
  }

}
