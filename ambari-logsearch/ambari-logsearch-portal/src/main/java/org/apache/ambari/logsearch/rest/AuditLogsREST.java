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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.manager.AuditMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.*;
import static org.apache.ambari.logsearch.doc.DocConstants.AuditOperationDescriptions.*;

@Api(value = "audit/logs", description = "Audit log operations")
@Path("audit/logs")
@Component
@Scope("request")
public class AuditLogsREST {

  @Autowired
  AuditMgr auditMgr;

  @GET
  @Path("/schema/fields")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_SCHEMA_FIELD_LIST_OD)
  public String getSolrFieldList(@Context HttpServletRequest request) {
    return auditMgr.getAuditLogsSchemaFieldsName();
  }

  @GET
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LOGS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = IS_LAST_PAGE_D, name = "isLastPage", dataType = "boolean", paramType = "query")
  })
  public String getAuditLogs(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("isLastPage", request.getParameter("isLastPage"));
    return auditMgr.getLogs(searchCriteria);
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_COMPONENTS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
  })
  public String getAuditComponents(@Context HttpServletRequest request) {

    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return auditMgr.getAuditComponents(searchCriteria);
  }

  @GET
  @Path("/linegraph")
  @Produces({"application/json"})
  @ApiOperation(GET_AUDIT_LINE_GRAPH_DATA_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UNIT_D, name = "unit", dataType = "string", paramType = "query")
  })
  public String getAuditLineGraphData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getAuditLineGraphData(searchCriteria);
  }

  @GET
  @Path("/users")
  @Produces({"application/json"})
  @ApiOperation(GET_TOP_AUDIT_USERS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FIELD_D, name = "field", dataType = "string", paramType = "query")
  })
  public String getTopAuditUsers(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    return auditMgr.topTenUsers(searchCriteria);
  }

  @GET
  @Path("/resources")
  @Produces({"application/json"})
  @ApiOperation(GET_TOP_AUDIT_RESOURCES_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FIELD_D, name = "field", dataType = "string", paramType = "query")
  })
  public String getTopAuditResources(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    //return auditMgr.getTopAuditFieldCount(searchCriteria);
    return auditMgr.topTenResources(searchCriteria);

  }

  @GET
  @Path("/live/count")
  @Produces({"application/json"})
  @ApiOperation(GET_LIVE_LOGS_COUNT_OD)
  public String getLiveLogsCount() {
    return auditMgr.getLiveLogCounts();
  }

  @GET
  @Path("/request/user/linegraph")
  @Produces({"application/json"})
  @ApiOperation(GET_REQUEST_USER_LINE_GRAPH_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FIELD_D, name = "field", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UNIT_D, name = "unit", dataType = "string", paramType = "query")
  })
  public String getRequestUserLineGraph(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getRequestUserLineGraph(searchCriteria);
  }

  @GET
  @Path("/anygraph")
  @Produces({"application/json"})
  @ApiOperation(GET_ANY_GRAPH_DATA_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = X_AXIS_D, name = "xAxis", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = Y_AXIS_D, name = "yAxis", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = STACK_BY_D, name = "stackBy", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UNIT_D, name = "unit", dataType = "string", paramType = "query")
  })
  public String getAnyGraphData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("xAxis", request.getParameter("xAxis"));
    searchCriteria.addParam("yAxis", request.getParameter("yAxis"));
    searchCriteria.addParam("stackBy", request.getParameter("stackBy"));
    searchCriteria.addParam("from", request.getParameter("from"));
    searchCriteria.addParam("to", request.getParameter("to"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getAnyGraphData(searchCriteria);
  }

  @GET
  @Path("/users/export")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_USER_TALBE_TO_TEXT_FILE_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FIELD_D, name = "field", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FORMAT_D, name = "format", dataType = "string", paramType = "query")
  })
  public Response exportUserTableToTextFile(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    searchCriteria.addParam("format", request.getParameter("format"));
    return auditMgr.exportUserTableToTextFile(searchCriteria);
  }

  @GET
  @Path("/serviceload")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOAD_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
  })
  public String getServiceLoad(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    return auditMgr.getServiceLoad(searchCriteria);
  }

}
