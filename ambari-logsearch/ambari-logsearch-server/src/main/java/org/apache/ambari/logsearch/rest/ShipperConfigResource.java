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
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.apache.ambari.logsearch.manager.ShipperConfigManager;
import org.apache.ambari.logsearch.model.common.LSServerInputConfig;
import org.apache.ambari.logsearch.model.common.LSServerLogLevelFilterMap;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.GET_LOG_LEVEL_FILTER_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.GET_SERVICE_NAMES_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.GET_SHIPPER_CONFIG_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.SET_SHIPPER_CONFIG_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.TEST_SHIPPER_CONFIG_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.UPDATE_LOG_LEVEL_FILTER_OD;

@Api(value = "shipper", description = "Shipper config operations")
@Path("shipper")
@Named
@Scope("request")
public class ShipperConfigResource {

  @Inject
  private ShipperConfigManager shipperConfigManager;

  @GET
  @Path("/input/{clusterName}/services")
  @Produces({"application/json"})
  @ApiOperation(GET_SERVICE_NAMES_OD)
  public List<String> getServices(@PathParam("clusterName") String clusterName) {
    return shipperConfigManager.getServices(clusterName);
  }

  @GET
  @Path("/input/{clusterName}/services/{serviceName}")
  @Produces({"application/json"})
  @ApiOperation(GET_SHIPPER_CONFIG_OD)
  public LSServerInputConfig getShipperConfig(@PathParam("clusterName") String clusterName, @PathParam("serviceName")
    String serviceName) {
    return shipperConfigManager.getInputConfig(clusterName, serviceName);
  }

  @POST
  @Path("/input/{clusterName}/services/{serviceName}")
  @Produces({"application/json"})
  @ApiOperation(SET_SHIPPER_CONFIG_OD)
  @ValidateOnExecution
  public Response createShipperConfig(@Valid LSServerInputConfig request, @PathParam("clusterName") String clusterName,
      @PathParam("serviceName") String serviceName) {
    return shipperConfigManager.createInputConfig(clusterName, serviceName, request);
  }

  @PUT
  @Path("/input/{clusterName}/services/{serviceName}")
  @Produces({"application/json"})
  @ApiOperation(SET_SHIPPER_CONFIG_OD)
  @ValidateOnExecution
  public Response setShipperConfig(@Valid LSServerInputConfig request, @PathParam("clusterName") String clusterName,
      @PathParam("serviceName") String serviceName) {
    return shipperConfigManager.setInputConfig(clusterName, serviceName, request);
  }

  @POST
  @Path("/input/{clusterName}/test")
  @Produces({"application/json"})
  @ApiOperation(TEST_SHIPPER_CONFIG_OD)
  public Map<String, Object> testShipperConfig(@FormParam("shipper_config") String shipperConfig, @FormParam("log_id") String logId,
      @FormParam("test_entry") String testEntry, @PathParam("clusterName") String clusterName) {
    return shipperConfigManager.testShipperConfig(shipperConfig, logId, testEntry, clusterName);
  }

  @GET
  @Path("/filters/{clusterName}/level")
  @Produces({"application/json"})
  @ApiOperation(GET_LOG_LEVEL_FILTER_OD)
  public LSServerLogLevelFilterMap getLogLevelFilters(@PathParam("clusterName") String clusterName) {
    return shipperConfigManager.getLogLevelFilters(clusterName);
  }

  @PUT
  @Path("/filters/{clusterName}/level")
  @Produces({"application/json"})
  @ApiOperation(UPDATE_LOG_LEVEL_FILTER_OD)
  @ValidateOnExecution
  public Response setLogLevelFilter(@Valid LSServerLogLevelFilterMap request, @PathParam("clusterName") String clusterName) {
    return shipperConfigManager.setLogLevelFilters(clusterName, request);
  }

}
