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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.manager.InfoManager;
import org.apache.ambari.logsearch.model.response.PropertyDescriptionData;
import org.apache.ambari.logsearch.model.response.ShipperConfigDescriptionData;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_ALL_PROPERTIES_INFO_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_ALL_SHIPPER_CONFIG_INFO_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_APP_DETAILS_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_FEATURES_LIST;
import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_LOGSEARCH_PROPERTIES_INFO_OD;
import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.GET_AUTH_DETAILS_OD;

@Api(value = "info", description = "General configuration information")
@Path("info")
@Named
@Scope("request")
public class InfoResource {

  @Inject
  private InfoManager infoManager;

  @GET
  @Produces({"application/json"})
  @ApiOperation(GET_APP_DETAILS_OD)
  public Map<String, String> getApplicationInfo() {
    return infoManager.getApplicationInfo();
  }

  @GET
  @Path("/properties")
  @Produces({"application/json"})
  @ApiOperation(GET_ALL_PROPERTIES_INFO_OD)
  public Map<String, List<PropertyDescriptionData>> getPropertyDescriptions() {
    return infoManager.getPropertyDescriptions();
  }

  @GET
  @Path("/properties/{propertyFile}")
  @Produces({"application/json"})
  @ApiOperation(GET_LOGSEARCH_PROPERTIES_INFO_OD)
  public List<PropertyDescriptionData> getPropertyFileDescription(@PathParam("propertyFile") String propertyFile) {
    return infoManager.getLogSearchPropertyDescriptions(propertyFile);
  }

  @GET
  @Path("/features")
  @Produces({"application/json"})
  @ApiOperation(GET_FEATURES_LIST)
  public Map<String, Object> getFeatures() {
    return infoManager.getFeaturesMap();
  }

  @GET
  @Path("/features/auth")
  @Produces({"application/json"})
  @ApiOperation(GET_AUTH_DETAILS_OD)
  public Map<String, Boolean> getAuthInfo() {
    return infoManager.getAuthMap();
  }

  @GET
  @Path("/shipperconfig")
  @Produces({"application/json"})
  @ApiOperation(GET_ALL_SHIPPER_CONFIG_INFO_OD)
  public List<ShipperConfigDescriptionData> getShipperConfigDescription() {
    return infoManager.getLogSearchShipperConfigDescription();
  }
}
