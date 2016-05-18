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
package org.apache.ambari.logsearch.web.security;

import java.util.HashMap;

import org.apache.ambari.logsearch.dao.UserDao;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class LogsearchAuthenticationProvider extends
  LogsearchAbstractAuthenticationProvider {
  private static final Logger logger = Logger
    .getLogger(LogsearchAuthenticationProvider.class);
  private static Logger auditLogger = Logger
    .getLogger("org.apache.ambari.logsearch.audit");

  @Autowired
  UserDao userDao;

  @Autowired
  LogsearchLdapAuthenticationProvider ldapAuthenticationProvider;

  @Autowired
  LogsearchFileAuthenticationProvider fileAuthenticationProvider;

  @Autowired
  LogsearchSimpleAuthenticationProvider simpleAuthenticationProvider;
  
  @Autowired
  LogsearchExternalServerAuthenticationProvider externalServerAuthenticationProvider;

  @Autowired
  JSONUtil jsonUtil;

  @Autowired
  private UserDetailsService userService;

  @Override
  public Authentication authenticate(Authentication authentication)
    throws AuthenticationException {
    logger.info("Authenticating user:" + authentication.getName()
      + ", userDetail=" + authentication.toString());
    Authentication inAuthentication = authentication;
    AuthenticationException authException = null;
    HashMap<String, Object> auditRecord = new HashMap<String, Object>();
    auditRecord.put("user", authentication.getName());
    auditRecord.put("principal", authentication.getPrincipal().toString());
    auditRecord.put("auth_class", authentication.getClass().getName());
    logger.info("authentication.class="
      + authentication.getClass().getName());
    if (inAuthentication instanceof UsernamePasswordAuthenticationToken) {
      UsernamePasswordAuthenticationToken authClass = (UsernamePasswordAuthenticationToken) inAuthentication;
      Object details = authClass.getDetails();
      if (details instanceof WebAuthenticationDetails) {
        WebAuthenticationDetails webAuthentication = (WebAuthenticationDetails) details;
        auditRecord.put("remote_ip",
          webAuthentication.getRemoteAddress());
        auditRecord.put("session", webAuthentication.getSessionId());
      }
    }
    boolean isSuccess = false;
    try {
      for (AUTH_METHOD authMethod : AUTH_METHOD.values()) {
        try {
          authentication = doAuth(authentication, authMethod);
          if (authentication != null
            && authentication.isAuthenticated()) {
            logger.info("Authenticated using method="
              + authMethod.name() + ", user="
              + authentication.getName());
            auditRecord.put("result", "allowed");
            isSuccess = true;
            auditRecord.put("authType", authMethod.name());
            return authentication;
          }
        } catch (AuthenticationException ex) {
          if (authException == null) {
            // Let's save the first one
            authException = ex;
          }
        }
      }
      auditRecord.put("result", "denied");
      logger.warn("Authentication failed for user="
        + inAuthentication.getName() + ", userDetail="
        + inAuthentication.toString());
      if (authException != null) {
        auditRecord.put("reason", authException.getMessage());
        throw authException;
      }
      return authentication;
    } finally {
      String jsonStr = jsonUtil.mapToJSON(auditRecord);
      if (isSuccess) {
        auditLogger.info(jsonStr);
      } else {
        auditLogger.warn(jsonStr);
      }
    }
  }

  /**
   * @param authentication
   * @param authMethod
   * @return
   */
  public Authentication doAuth(Authentication authentication, AUTH_METHOD authMethod) {
    if (authMethod.equals(AUTH_METHOD.LDAP)) {
      authentication = ldapAuthenticationProvider.authenticate(authentication);
    } else if (authMethod.equals(AUTH_METHOD.FILE)) {
      authentication = fileAuthenticationProvider.authenticate(authentication);
    } else if (authMethod.equals(AUTH_METHOD.SIMPLE)) {
      authentication = simpleAuthenticationProvider.authenticate(authentication);
    }else if (authMethod.equals(AUTH_METHOD.EXTERNAL_AUTH)) {
      authentication = externalServerAuthenticationProvider.authenticate(authentication);
    } else {
      logger.error("Invalid authentication method :" + authMethod.name());
    }
    return authentication;
  }
}
