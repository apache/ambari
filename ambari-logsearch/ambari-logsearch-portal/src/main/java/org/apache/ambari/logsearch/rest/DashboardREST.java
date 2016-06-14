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

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.manager.LogsMgr;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.view.VCountList;
import org.apache.ambari.logsearch.view.VNameValueList;
import org.apache.ambari.logsearch.view.VNodeList;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("dashboard")
@Component
@Scope("request")
public class DashboardREST {

  @Autowired
  LogsMgr logMgr;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @GET
  @Path("/solr/logs_search")
  @Produces({"application/json"})
  public String searchSolrData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("keyword", StringEscapeUtils.unescapeXml(request.getParameter("find")));
    searchCriteria.addParam("sourceLogId", request.getParameter("sourceLogId"));
    searchCriteria.addParam("keywordType",
      request.getParameter("keywordType"));
    searchCriteria.addParam("token",
      request.getParameter("token"));
    searchCriteria.addParam("isLastPage",request.getParameter("isLastPage"));
    return logMgr.searchLogs(searchCriteria);
  }

  @GET
  @Path("/hosts")
  @Produces({"application/json"})
  public String getHosts(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return logMgr.getHosts(searchCriteria);
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  public String getComponents(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return logMgr.getComponents(searchCriteria);
  }

  @GET
  @Path("/aggregatedData")
  @Produces({"application/json"})
  public String getAggregatedInfo(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addRequiredServiceLogsParams(request);
    return logMgr.getAggregatedInfo(searchCriteria);
  }

  @GET
  @Path("/levels_count")
  @Produces({"application/json"})
  public VCountList getLogLevelsCount(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addParam("q", request.getParameter("q"));
    searchCriteria
      .addParam("startDate", request.getParameter("start_time"));
    searchCriteria.addParam("endDate", request.getParameter("end_time"));
    return logMgr.getLogLevelCount(searchCriteria);
  }

  @GET
  @Path("/components_count")
  @Produces({"application/json"})
  public VCountList getComponentsCount(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addParam("q", request.getParameter("q"));
    searchCriteria
      .addParam("startDate", request.getParameter("start_time"));
    searchCriteria.addParam("endDate", request.getParameter("end_time"));
    return logMgr.getComponentsCount(searchCriteria);
  }

  @GET
  @Path("/hosts_count")
  @Produces({"application/json"})
  public VCountList getHostsCount(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addParam("q", request.getParameter("q"));
    searchCriteria
      .addParam("startDate", request.getParameter("start_time"));
    searchCriteria.addParam("endDate", request.getParameter("end_time"));
    searchCriteria.addParam("excludeQuery", StringEscapeUtils
      .unescapeXml(request.getParameter("excludeQuery")));
    searchCriteria.addParam("includeQuery", StringEscapeUtils
      .unescapeXml(request.getParameter("includeQuery")));
    return logMgr.getHostsCount(searchCriteria);
  }

  @GET
  @Path("/getTreeExtension")
  @Produces({"application/json"})
  public VNodeList getTreeExtension(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("hostName", request.getParameter("hostName"));
    return logMgr.getTreeExtension(searchCriteria);
  }

  @GET
  @Path("/getLogLevelCounts")
  @Produces({"application/json"})
  public VNameValueList getLogsLevelCount(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    return logMgr.getLogsLevelCount(searchCriteria);
  }

  @GET
  @Path("/getHistogramData")
  @Produces({"application/json"})
  public String getHistogramData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return logMgr.getHistogramData(searchCriteria);
  }

  @GET
  @Path("/cancelFindRequest")
  @Produces({"application/json"})
  public String cancelFindRequest(@Context HttpServletRequest request) {
    return logMgr.cancelFindRequestByDate(request);
  }

  @GET
  @Path("/exportToTextFile")
  @Produces({"application/json"})
  public Response exportToTextFile(@Context HttpServletRequest request) {

    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("format", request.getParameter("format"));
    searchCriteria.addParam("utcOffset", request.getParameter("utcOffset"));
    return logMgr.exportToTextFile(searchCriteria);

  }

  @GET
  @Path("/getHostListByComponent")
  @Produces({"application/json"})
  public String getHostListByComponent(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("componentName",
      request.getParameter("componentName"));
    return logMgr.getHostListByComponent(searchCriteria);
  }

  @GET
  @Path("/getComponentListWithLevelCounts")
  @Produces({"application/json"})
  public String getComponentListWithLevelCounts(
    @Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    return logMgr.getComponentListWithLevelCounts(searchCriteria);
  }

  @GET
  @Path("/solr/getBundleIdBoundaryDates")
  @Produces({"application/json"})
  public String getExtremeDatesForBundelId(@Context HttpServletRequest request) {

    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam(LogSearchConstants.BUNDLE_ID,
      request.getParameter("bundle_id"));

    return logMgr.getExtremeDatesForBundelId(searchCriteria);

  }

  @GET
  @Path("/getServiceLogsFieldsName")
  @Produces({"application/json"})
  public String getServiceLogsFieldsName() {
    return logMgr.getServiceLogsFieldsName();
  }

  @GET
  @Path("/getServiceLogsSchemaFieldsName")
  @Produces({"application/json"})
  public String getServiceLogsSchemaFieldsName() {
    return logMgr.getServiceLogsSchemaFieldsName();
  }

  @GET
  @Path("/getAnyGraphData")
  @Produces({"application/json"})
  public String getAnyGraphData(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("xAxis", request.getParameter("xAxis"));
    searchCriteria.addParam("yAxis", request.getParameter("yAxis"));
    searchCriteria.addParam("stackBy", request.getParameter("stackBy"));
    searchCriteria.addParam("from", request.getParameter("from"));
    searchCriteria.addParam("to", request.getParameter("to"));
    searchCriteria.addParam("unit", request.getParameter("unit"));
    return logMgr.getAnyGraphData(searchCriteria);
  }

  @GET
  @Path("/getAfterBeforeLogs")
  @Produces({"application/json"})
  public String getAfterBeforeLogs(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addRequiredServiceLogsParams(request);
    searchCriteria.addParam("hostLogFile", request.getParameter("host"));
    searchCriteria.addParam("compLogFile",
      request.getParameter("component"));
    searchCriteria.addParam("id", request.getParameter("id"));
    searchCriteria.addParam("scrollType",
      request.getParameter("scrollType"));
    searchCriteria.addParam("numberRows",
      request.getParameter("numberRows"));
    return logMgr.getAfterBeforeLogs(searchCriteria);
  }

  @GET
  @Path("/getHadoopServiceConfigJSON")
  @Produces({"application/json"})
  public String getHadoopServiceConfigJSON() {
    return logMgr.getHadoopServiceConfigJSON();
  }
}