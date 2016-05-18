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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.manager.UserConfigMgr;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.view.VUserConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("userconfig")
@Component
@Scope("request")
public class UserConfigREST {

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  UserConfigMgr userConfigMgr;

  @POST
  @Path("/saveUserConfig")
  @Produces({"application/json"})
  public String saveUserConfig(VUserConfig vhist) {
    return userConfigMgr.saveUserConfig(vhist);
  }

  @PUT
  @Path("/updateUserConfig")
  @Produces({"application/json"})
  public String updateUserConfig(VUserConfig vhist) {
    return userConfigMgr.updateUserConfig(vhist);
  }

  @DELETE
  @Path("/deleteUserConfig/{id}")
  public void deleteUserConfig(@PathParam("id") String id) {
    userConfigMgr.deleteUserConfig(id);
  }

  @GET
  @Path("/getUserConfig")
  @Produces({"application/json"})
  public String getUserConfig(@Context HttpServletRequest request) {
    SearchCriteria searchCriteria = new SearchCriteria(request);
    searchCriteria.addParam(LogSearchConstants.USER_NAME,
      request.getParameter("userId"));
    searchCriteria.addParam(LogSearchConstants.FILTER_NAME,
      request.getParameter("filterName"));
    searchCriteria.addParam(LogSearchConstants.ROW_TYPE,
      request.getParameter("rowType"));
    return userConfigMgr.getUserConfig(searchCriteria);
  }

  @GET
  @Path("/user_filter")
  @Produces({"application/json"})
  public String getUserFilter(@Context HttpServletRequest request) {
    return userConfigMgr.getUserFilter();
  }

  @POST
  @Path("/user_filter")
  @Produces({"application/json"})
  public String createUserFilter(String json) {
    return userConfigMgr.saveUserFiter(json);
  }

  @PUT
  @Path("/user_filter/{id}")
  @Produces({"application/json"})
  public String updateUserFilter(String json) {
    return userConfigMgr.saveUserFiter(json);
  }

  @GET
  @Path("/getAllUserName")
  @Produces({"application/json"})
  public String getAllUserName() {
    return userConfigMgr.getAllUserName();
  }

}
