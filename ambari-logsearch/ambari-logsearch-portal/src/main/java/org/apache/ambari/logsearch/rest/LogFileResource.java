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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.model.request.impl.LogFileRequest;
import org.apache.ambari.logsearch.model.request.impl.LogFileTailRequest;
import org.apache.ambari.logsearch.model.response.LogFileDataListResponse;
import org.apache.ambari.logsearch.model.response.LogListResponse;
import org.apache.ambari.logsearch.query.model.LogFileSearchCriteria;
import org.apache.ambari.logsearch.query.model.LogFileTailSearchCriteria;
import org.apache.ambari.logsearch.manager.LogFileManager;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.LogFileOperationDescriptions.*;

@Api(value = "logfile", description = "Logfile operations")
@Path("logfile")
@Component
@Scope("request")
public class LogFileResource {

  @Inject
  private LogFileManager logFileManager;

  @Inject
  private ConversionService conversionService;

  @GET
  @Produces({"application/json"})
  @ApiOperation(SEARCH_LOG_FILES_OD)
  public LogFileDataListResponse searchLogFiles(@BeanParam LogFileRequest request) {
    return logFileManager.searchLogFiles(conversionService.convert(request, LogFileSearchCriteria.class));
  }

  @GET
  @Path("/tail")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_FILE_TAIL_OD)
  public LogListResponse getLogFileTail(@BeanParam LogFileTailRequest request) {
    return logFileManager.getLogFileTail(conversionService.convert(request, LogFileTailSearchCriteria.class));
  }

}
