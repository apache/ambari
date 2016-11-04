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

package org.apache.ambari.view.hive2;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds session parameters pulled from the
 * view context
 */
public class AuthParams {
  private static final String HIVE_SESSION_PARAMS = "hive.session.params";
  private Map<String, String> sessionParams = new HashMap<>();
  private final ViewContext context;

  public AuthParams(ViewContext context) {
    sessionParams = parseSessionParams(context.getProperties().get(HIVE_SESSION_PARAMS));
    this.context = context;
  }

  /**
   * Returns a map created by parsing the parameters in view context
   * @param params session parameters as string
   * @return parsed session parameters
   */
  private Map<String, String> parseSessionParams(String params) {
    Map<String, String> sessions = new HashMap<>();
    if (StringUtils.isEmpty(params))
      return sessions;
    String[] splits = params.split(";");
    for (String split : splits) {
      String[] paramSplit = split.trim().split("=");
      if ("auth".equals(paramSplit[0]) || "proxyuser".equals(paramSplit[0])) {
        sessions.put(paramSplit[0], paramSplit[1]);
      }
    }
    return Collections.unmodifiableMap(sessions);
  }

  /**
   * Gets the proxy user
   * @return User and group information
   * @throws IOException
   */
  public UserGroupInformation getProxyUser() throws IOException {
    UserGroupInformation ugi;
    String proxyuser = null;
    if(context.getCluster() != null) {
      proxyuser = context.getCluster().getConfigurationValue("cluster-env","ambari_principal_name");
    }

    if(StringUtils.isEmpty(proxyuser)) {
      if (sessionParams.containsKey("proxyuser")) {
        ugi = UserGroupInformation.createRemoteUser(sessionParams.get("proxyuser"));
      } else {
        ugi = UserGroupInformation.getCurrentUser();
      }
    } else {
      ugi = UserGroupInformation.createRemoteUser(proxyuser);
    }
    ugi.setAuthenticationMethod(getAuthenticationMethod());
    return ugi;
  }

  /**
   * Get the Authentication method
   * @return
   */
  private UserGroupInformation.AuthenticationMethod getAuthenticationMethod() {
    UserGroupInformation.AuthenticationMethod authMethod;
    if (sessionParams.containsKey("auth") && !StringUtils.isEmpty(sessionParams.get("auth"))) {
      String authName = sessionParams.get("auth");
      authMethod = UserGroupInformation.AuthenticationMethod.valueOf(authName.toUpperCase());
    } else {
      authMethod = UserGroupInformation.AuthenticationMethod.SIMPLE;
    }
    return authMethod;
  }
}
