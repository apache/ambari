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
package org.apache.ambari.infra.security;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.apache.hadoop.security.authentication.util.KerberosName;

import java.security.Principal;
import java.util.Map;
import java.util.Set;


/**
 * Strategy class to get roles with the principal name (in a specific format e.g.: 'name@DOMAIN')
 * in case of KerberosPlugin is used for authentication
 */
public class InfraUserRolesLookupStrategy {

  public Set<String> getUserRolesFromPrincipal(Map<String, Set<String>> usersVsRoles, Principal principal) {
    if (principal instanceof AuthenticationToken) {
      AuthenticationToken authenticationToken = (AuthenticationToken) principal;
      KerberosName kerberosName = new KerberosName(authenticationToken.getName());
      Set<String> rolesResult = usersVsRoles.get(String.format("%s@%s", kerberosName.getServiceName(), kerberosName.getRealm()));
      if (CollectionUtils.isEmpty(rolesResult)) {
        rolesResult = usersVsRoles.get(principal.getName());
      }
      return rolesResult;
    } else {
      return usersVsRoles.get(principal.getName());
    }
  }
}
