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

package org.apache.ambari.view.hive20.utils;

import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.ambari.NoClusterAssociatedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Utility class to check the authorization of the user
 */
public class AuthorizationChecker {
  protected final Logger LOG = LoggerFactory.getLogger(getClass());
  private static final String AMBARI_OR_CLUSTER_ADMIN_PRIVILEGE_URL = "/api/v1/users/%s?privileges/PrivilegeInfo/permission_name=AMBARI.ADMINISTRATOR|" +
      "(privileges/PrivilegeInfo/permission_name.in(CLUSTER.ADMINISTRATOR,CLUSTER.OPERATOR)&privileges/PrivilegeInfo/cluster_name=%s)";

  private final ViewContext viewContext;
  private final AmbariApi ambariApi;


  @Inject
  public AuthorizationChecker(ViewContext viewContext) {
    this.viewContext = viewContext;
    this.ambariApi = new AmbariApi(viewContext);
  }

  public boolean isOperator() {
    if (viewContext.getCluster() == null) {
      throw new NoClusterAssociatedException("No cluster is associated with the current instance");
    }
    String fetchUrl = String.format(AMBARI_OR_CLUSTER_ADMIN_PRIVILEGE_URL, viewContext.getUsername(), viewContext.getCluster().getName());

    try {
      String response = ambariApi.readFromAmbari(fetchUrl, "GET", null, null);

      if (response != null && !response.isEmpty()) {
        JSONObject json = (JSONObject) JSONValue.parse(response);
        if (json.containsKey("privileges")) {
          JSONArray privileges = (JSONArray) json.get("privileges");
          if (privileges.size() > 0) return true;
        }
      }

    } catch (AmbariHttpException e) {
      LOG.error("Got Error response from url : {}. Response : {}", fetchUrl, e.getMessage(), e);
    }

    return false;
  }
}
