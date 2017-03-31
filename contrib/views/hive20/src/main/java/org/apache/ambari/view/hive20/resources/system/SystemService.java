/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.ambari.view.hive20.BaseService;
import org.apache.ambari.view.hive20.resources.system.ranger.RangerService;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.json.simple.JSONObject;

/**
 * System services which are required for the working of the application
 */
public class SystemService extends BaseService {

  private final RangerService rangerService;

  @Inject
  public SystemService(RangerService rangerService) {
    this.rangerService = rangerService;
  }


  /**
   * Returns if the current user is a cluster operator or ambari administrator
   */
  @GET
  @Path("/ranger/auth")
  public Response rangerAuth(@QueryParam("database") String database,
                             @QueryParam("table") String table) {

    List<RangerService.Policy> policies = rangerService.getPolicies(database, table);
    JSONObject response = new JSONObject();
    response.put("policies", policies);
    return Response.ok(response).build();
  }

  @GET
  @Path("/service-check-policy")
  public Response getServiceCheckList(){
    ServiceCheck serviceCheck = new ServiceCheck(context);
    try {
      ServiceCheck.Policy policy = serviceCheck.getServiceCheckPolicy();
      JSONObject policyJson = new JSONObject();
      policyJson.put("serviceCheckPolicy", policy);
      return Response.ok(policyJson).build();
    } catch (HdfsApiException e) {
      LOG.error("Error occurred while generating service check policy : ", e);
      throw new ServiceFormattedException(e);
    }
  }
}
