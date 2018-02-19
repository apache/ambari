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
package org.apache.solr.security;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.apache.hadoop.security.authentication.util.KerberosName;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * Validate that the user has the right access based on the hostname in the kerberos principal
 */
public class InfraKerberosHostValidator {

  public boolean validate(Principal principal, Map<String, Set<String>> userVsHosts, Map<String, String> userVsHostRegex) {
    if (principal instanceof AuthenticationToken) {
      AuthenticationToken authenticationToken = (AuthenticationToken) principal;
      KerberosName kerberosName = new KerberosName(authenticationToken.getName());
      String hostname = kerberosName.getHostName();
      String serviceUserName = kerberosName.getServiceName();
      if (MapUtils.isNotEmpty(userVsHostRegex)) {
        String regex = userVsHostRegex.get(serviceUserName);
        return hostname.matches(regex);
      }
      if (MapUtils.isNotEmpty(userVsHosts)) {
        Set<String> hosts = userVsHosts.get(serviceUserName);
        if (CollectionUtils.isNotEmpty(hosts)) {
          return hosts.contains(hostname);
        }
      }
    }
    return true;
  }
}
