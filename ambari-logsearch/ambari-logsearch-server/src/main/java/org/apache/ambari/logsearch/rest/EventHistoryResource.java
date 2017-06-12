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
import javax.inject.Named;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.manager.EventHistoryManager;
import org.apache.ambari.logsearch.model.request.impl.EventHistoryRequest;
import org.apache.ambari.logsearch.model.response.EventHistoryData;
import org.apache.ambari.logsearch.model.response.EventHistoryDataListResponse;
import org.springframework.context.annotation.Scope;

import java.util.List;

import static org.apache.ambari.logsearch.doc.DocConstants.EventHistoryOperationDescriptions.*;

@Api(value = "history", description = "Event history operations")
@Path("history")
@Named
@Scope("request")
public class EventHistoryResource {

  @Inject
  private EventHistoryManager eventHistoryManager;

  @POST
  @Produces({"application/json"})
  @ApiOperation(SAVE_EVENT_HISTORY_DATA_OD)
  public String saveEvent(EventHistoryData eventHistoryData) {
    return eventHistoryManager.saveEvent(eventHistoryData);
  }

  @DELETE
  @Path("/{id}")
  @ApiOperation(DELETE_EVENT_HISTORY_DATA_OD)
  public void deleteEvent(@PathParam("id") String id) {
    eventHistoryManager.deleteEvent(id);
  }

  @GET
  @Produces({"application/json"})
  @ApiOperation(GET_EVENT_HISTORY_DATA_OD)
  public EventHistoryDataListResponse getEventHistory(@BeanParam EventHistoryRequest request) {
    return eventHistoryManager.getEventHistory(request);
  }

  @GET
  @Path("/names")
  @Produces({"application/json"})
  @ApiOperation(GET_ALL_USER_NAMES_OD)
  public List<String> getAllUserName() {
    return eventHistoryManager.getAllUserName();
  }

}
