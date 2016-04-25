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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.manager.AuditMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("audit")
@Component
@Scope("request")
public class AuditREST {

  @Autowired
  AuditMgr auditMgr;

  @GET
  @Path("/getAuditSchemaFieldsName")
  @Produces({"application/json"})
  public String getSolrFieldList(@Context HttpServletRequest request) {
    return auditMgr.getAuditLogsSchemaFieldsName();
  }

  @GET
  @Path("/getAuditLogs")
  @Produces({"application/json"})
  public String getAuditLogs(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    return auditMgr.getLogs(searchCriteria);
  }

  @GET
  @Path("/getAuditComponents")
  @Produces({"application/json"})
  public String getAuditComponents(@Context HttpServletRequest request) {

    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return auditMgr.getAuditComponents(searchCriteria);
  }

  @GET
  @Path("/getAuditLineGraphData")
  @Produces({"application/json"})
  public String getAuditLineGraphData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getAuditLineGraphData(searchCriteria);
  }

  @GET
  @Path("/getTopAuditUsers")
  @Produces({"application/json"})
  public String getTopAuditUsers(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    return auditMgr.topTenUsers(searchCriteria);
  }

  @GET
  @Path("/getTopAuditResources")
  @Produces({"application/json"})
  public String getTopAuditResources(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    //return auditMgr.getTopAuditFieldCount(searchCriteria);
    return auditMgr.topTenResources(searchCriteria);


  }

  @GET
  @Path("/getTopAuditComponents")
  @Produces({"application/json"})
  public String getTopAuditComponents(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getTopAuditFieldCount(searchCriteria);
  }

  @GET
  @Path("/getLiveLogsCount")
  @Produces({"application/json"})
  public String getLiveLogsCount() {
    return auditMgr.getLiveLogCounts();
  }

  @GET
  @Path("/getRequestUserLineGraph")
  @Produces({"application/json"})
  public String getRequestUserLineGraph(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return auditMgr.getRequestUserLineGraph(searchCriteria);
  }

  @GET
  @Path("/getAnyGraphData")
  @Produces({"application/json"})
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
  @Path("/exportUserTableToTextFile")
  @Produces({"application/json"})
  public Response exportUserTableToTextFile(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    searchCriteria.addParam("field", request.getParameter("field"));
    searchCriteria.addParam("format", request.getParameter("format"));
    return auditMgr.exportUserTableToTextFile(searchCriteria);
  }

  @GET
  @Path("/getServiceLoad")
  @Produces({"application/json"})
  public String getServiceLoad(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredAuditLogsParams(request);
    return auditMgr.getServiceLoad(searchCriteria);
  }

}
 