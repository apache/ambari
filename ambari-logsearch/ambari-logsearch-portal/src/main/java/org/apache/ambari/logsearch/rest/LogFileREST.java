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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.manager.LogFileMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.LogFileDescriptions.*;
import static org.apache.ambari.logsearch.doc.DocConstants.LogFileOperationDescriptions.*;

@Api(value = "logfile", description = "Logfile operations")
@Path("logfile")
@Component
@Scope("request")
public class LogFileREST {

  @Autowired
  LogFileMgr logFileMgr;

  @GET
  @Produces({"application/json"})
  @ApiOperation(SEARCH_LOG_FILES_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LOG_TYPE_D, name = "logType", dataType = "string", paramType = "query")
  })
  public String searchLogFiles(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam("component", request.getParameter("component"));
    searchCriteria.addParam("host", request.getParameter("host"));
    searchCriteria.addParam("logType", request.getParameter("logType"));
    return logFileMgr.searchLogFiles(searchCriteria);
  }

  @GET
  @Path("/getLogFileTail")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_FILE_TAIL_OD)
  @ApiImplicitParams(value = {
    @ApiImplicitParam(value = COMPONENT_D, name = "component", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = HOST_D, name = "host", dataType = "string", paramType = "query"),
    @ApiImplicitParam(value = LOG_TYPE_D, name = "logType", dataType = "string", paramType = "query")
  })
  public String getLogFileTail(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.addParam("host", request.getParameter("host"));
    searchCriteria.addParam("component", request.getParameter("component"));
    searchCriteria.addParam("name", request.getParameter("name"));
    searchCriteria.addParam("tailSize", request.getParameter("tailSize"));
    return logFileMgr.getLogFileTail(searchCriteria);
  }

}
