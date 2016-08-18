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

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ambari.logsearch.manager.PublicMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.apache.ambari.logsearch.doc.DocConstants.PublicOperationDescriptions.OBTAIN_GENERAL_CONFIG_OD;

@Api(value = "public", description = "Public operations")
@Path("public")
@Component
@Scope("request")
public class PublicREST {

  @Autowired
  PublicMgr generalMgr;

  @GET
  @Path("/getGeneralConfig")
  @ApiOperation(OBTAIN_GENERAL_CONFIG_OD)
  public String getGeneralConfig() {
    return generalMgr.getGeneralConfig();
  }
}
