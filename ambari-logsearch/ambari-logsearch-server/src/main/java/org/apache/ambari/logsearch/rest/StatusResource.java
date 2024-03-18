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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.HashMap;
import java.util.Map;

import static org.apache.ambari.logsearch.doc.DocConstants.StatusOperationDescriptions.AUDIT_LOGS_STATUS_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.StatusOperationDescriptions.SERVICE_LOGS_STATUS_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.StatusOperationDescriptions.STATUS_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.StatusOperationDescriptions.EVENT_HISTORY_STATUS_OD;

@Api(value = "status", description = "Status Operations")
@Path("status")
@Named
@Scope("request")
public class StatusResource {

  @Inject
  @Named("solrServiceLogsState")
  private SolrCollectionState solrServiceLogsState;

  @Inject
  @Named("solrAuditLogsState")
  private SolrCollectionState solrAuditLogsState;

  @Inject
  @Named("solrEventHistoryState")
  private SolrCollectionState solrEventHistoryState;

  @GET
  @Produces({"application/json"})
  @ApiOperation(STATUS_OD)
  public Map<String, SolrCollectionState> getStatus() {
    Map<String, SolrCollectionState> response = new HashMap<>();
    response.put("serviceLogs", solrServiceLogsState);
    response.put("auditLogs", solrAuditLogsState);
    response.put("eventHistory", solrEventHistoryState);
    return response;
  }

  @GET
  @Path("/servicelogs")
  @Produces({"application/json"})
  @ApiOperation(SERVICE_LOGS_STATUS_OD)
  public SolrCollectionState getServiceLogStatus() {
    return solrServiceLogsState;
  }

  @GET
  @Path("/auditlogs")
  @Produces({"application/json"})
  @ApiOperation(AUDIT_LOGS_STATUS_OD)
  public SolrCollectionState getSolrAuditLogsStatus() {
    return solrAuditLogsState;
  }

  @GET
  @Path("/history")
  @Produces({"application/json"})
  @ApiOperation(EVENT_HISTORY_STATUS_OD)
  public SolrCollectionState getSolrEventHistoryStatus() {
    return solrEventHistoryState;
  }
}
