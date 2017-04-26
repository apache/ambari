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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.manager.ShipperConfigManager;
import org.springframework.context.annotation.Scope;

import java.util.List;

import static org.apache.ambari.logsearch.doc.DocConstants.ShipperConfigOperationDescriptions.*;

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
  @ApiOperation(GET_SERVICE_NAMES)
  public List<String> getServices(@PathParam("clusterName") String clusterName) {
    return shipperConfigManager.getServices(clusterName);
  }

  @GET
  @Path("/input/{clusterName}/services/{serviceName}")
  @Produces({"application/json"})
  @ApiOperation(GET_SHIPPER_CONFIG)
  public String getShipperConfig(@PathParam("clusterName") String clusterName, @PathParam("serviceName") String serviceName) {
    return shipperConfigManager.getInputConfig(clusterName, serviceName);
  }

  @PUT
  @Path("/input/{clusterName}/services/{serviceName}")
  @Produces("text/plain")
  @ApiOperation(SET_SHIPPER_CONFIG)
  public Response setShipperConfig(String body, @PathParam("clusterName") String clusterName, @PathParam("serviceName")
    String serviceName) {
    return shipperConfigManager.setInputConfig(clusterName, serviceName, body);
  }
}
