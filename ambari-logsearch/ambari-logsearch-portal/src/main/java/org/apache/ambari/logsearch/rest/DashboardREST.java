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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.*;
import static org.apache.ambari.logsearch.doc.DocConstants.ServiceDescriptions.*;
import static org.apache.ambari.logsearch.doc.DocConstants.ServiceOperationDescriptions.*;

@Api(value = "dashboard", description = "Dashboard operations")
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
  @ApiOperation(SEARCH_LOGS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FIND_D, name = "find", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = SOURCE_LOG_ID_D, name = "sourceLogId", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = KEYWORD_TYPE_D, name = "keywordType", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TOKEN_D, name = "token", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = IS_LAST_PAGE_D, name = "isLastPage", dataType = "boolean", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_HOSTS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query")
  })
  public String getHosts(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return logMgr.getHosts(searchCriteria);
  }

  @GET
  @Path("/components")
  @Produces({"application/json"})
  @ApiOperation(GET_COMPONENTS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query")
  })
  public String getComponents(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("q", request.getParameter("q"));
    return logMgr.getComponents(searchCriteria);
  }

  @GET
  @Path("/aggregatedData")
  @Produces({"application/json"})
  @ApiOperation(GET_AGGREGATED_INFO_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
  public String getAggregatedInfo(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addRequiredServiceLogsParams(request);
    return logMgr.getAggregatedInfo(searchCriteria);
  }

  @GET
  @Path("/levels_count")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_COMPONENTS_COUNT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_HOSTS_COUNT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = QUERY_D, name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_TREE_EXTENSION_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_LOG_LEVELS_COUNT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_HISTOGRAM_DATA_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UNIT_D, name = "unit", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(CANCEL_FIND_REQUEST_OD)
  public String cancelFindRequest(@Context HttpServletRequest request) {
    return logMgr.cancelFindRequestByDate(request);
  }

  @GET
  @Path("/exportToTextFile")
  @Produces({"application/json"})
  @ApiOperation(EXPORT_TO_TEXT_FILE_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FORMAT_D, name = "format", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UTC_OFFSET_D, name = "utcOffset", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_HOST_LIST_BY_COMPONENT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_COMPONENT_LIST_WITH_LEVEL_COUNT_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_EXTREME_DATES_FOR_BUNDLE_ID_OD)
  public String getExtremeDatesForBundelId(@Context HttpServletRequest request) {

    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam(LogSearchConstants.BUNDLE_ID,
      request.getParameter("bundle_id"));

    return logMgr.getExtremeDatesForBundelId(searchCriteria);

  }

  @GET
  @Path("/getServiceLogsFieldsName")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOGS_FIELD_NAME_OD)
  public String getServiceLogsFieldsName() {
    return logMgr.getServiceLogsFieldsName();
  }

  @GET
  @Path("/getServiceLogsSchemaFieldsName")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_LOGS_SCHEMA_FIELD_NAME_OD)
  public String getServiceLogsSchemaFieldsName() {
    return logMgr.getServiceLogsSchemaFieldsName();
  }

  @GET
  @Path("/getAnyGraphData")
  @Produces({"application/json"})
  @ApiOperation(GET_ANY_GRAPH_DATA_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = X_AXIS_D, name = "xAxis", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = Y_AXIS_D, name = "yAxis", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = STACK_BY_D, name = "stackBy", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FROM_D, name = "from", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TO_D, name = "to", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = UNIT_D, name = "unit", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_AFTER_BEFORE_LOGS_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_D,name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ID_D, name = "id", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = SCROLL_TYPE_D, name = "scrollType", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = NUMBER_ROWS_D, name = "numberRows", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = ADVANCED_SEARCH_D, name = "advancedSearch", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = QUERY_D,name = "q", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LEVEL_D, name = "level", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = TREE_PARAMS_D, name = "treeParams", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COLUMN_QUERY_D, name = "columnQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = I_MESSAGE_D, name = "iMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_E_MESSAGE_D, name = "gEMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = E_MESSAGE_D, name = "eMessage", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = G_MUST_NOT_D, name = "gMustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_BE_D, name = "mustBe", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = MUST_NOT_D, name = "mustNot", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_NAME_D, name = "host_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = COMPONENT_NAME_D, name = "component_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = FILE_NAME_D, name = "file_name", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = EXCLUDE_QUERY_D, name = "excludeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = INCLUDE_QUERY_D, name = "includeQuery", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = START_TIME_D, name = "start_time", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = END_TIME_D, name = "end_time", dataType = "string", paramType = "query")
  })
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
  @ApiOperation(GET_HADOOP_SERVICE_CONFIG_JSON_OD)
  public String getHadoopServiceConfigJSON() {
    return logMgr.getHadoopServiceConfigJSON();
  }
}