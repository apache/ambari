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

import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LogsearchAuthenticationProvider extends LogsearchAbstractAuthenticationProvider {
  private static final Logger logger = Logger .getLogger(LogsearchAuthenticationProvider.class);
  private static final Logger auditLogger = Logger.getLogger("org.apache.ambari.logsearch.audit");

  @Inject
  private LogsearchFileAuthenticationProvider fileAuthenticationProvider;

  @Inject
  private LogsearchExternalServerAuthenticationProvider externalServerAuthenticationProvider;

  @Inject
  private LogsearchSimpleAuthenticationProvider simpleAuthenticationProvider;

  @Override
  public Authentication authenticate(Authentication inAuthentication) throws AuthenticationException {
    logger.info("Authenticating user:" + inAuthentication.getName() + ", userDetail=" + inAuthentication.toString());
    logger.info("authentication.class=" + inAuthentication.getClass().getName());

    HashMap<String, Object> auditRecord = new HashMap<String, Object>();
    auditRecord.put("user", inAuthentication.getName());
    auditRecord.put("principal", inAuthentication.getPrincipal().toString());
    auditRecord.put("auth_class", inAuthentication.getClass().getName());
    if (inAuthentication instanceof UsernamePasswordAuthenticationToken) {
      UsernamePasswordAuthenticationToken authClass = (UsernamePasswordAuthenticationToken) inAuthentication;
      Object details = authClass.getDetails();
      if (details instanceof WebAuthenticationDetails) {
        WebAuthenticationDetails webAuthentication = (WebAuthenticationDetails) details;
        auditRecord.put("remote_ip", webAuthentication.getRemoteAddress());
        auditRecord.put("session", webAuthentication.getSessionId());
      }
    }
    
    boolean isSuccess = false;
    try {
      Authentication authentication = inAuthentication;
      AuthenticationException authException = null;
      
      for (AuthMethod authMethod : AuthMethod.values()) {
        try {
          authentication = doAuth(authentication, authMethod);
          if (authentication != null && authentication.isAuthenticated()) {
            logger.info("Authenticated using method=" + authMethod.name() + ", user=" + authentication.getName());
            auditRecord.put("result", "allowed");
            isSuccess = true;
            auditRecord.put("authType", authMethod.name());
            return authentication;
          }
        } catch (AuthenticationException ex) {
          if (authException == null) {
            authException = ex;
          }
        } catch (Exception e) {
          logger.error(e, e.getCause());
        }
      }
      
      auditRecord.put("result", "denied");
      logger.warn("Authentication failed for user=" + inAuthentication.getName() + ", userDetail=" + inAuthentication.toString());
      if (authException != null) {
        auditRecord.put("reason", authException.getMessage());
        throw authException;
      }
      return authentication;
    } finally {
      String jsonStr = JSONUtil.mapToJSON(auditRecord);
      auditLogger.log(isSuccess ? Level.INFO : Level.WARN, jsonStr);
    }
  }

  private Authentication doAuth(Authentication authentication, AuthMethod authMethod) {
    switch (authMethod) {
      case FILE: return fileAuthenticationProvider.authenticate(authentication);
      case EXTERNAL_AUTH: return externalServerAuthenticationProvider.authenticate(authentication);
      case SIMPLE: return simpleAuthenticationProvider.authenticate(authentication);
      default: logger.error("Invalid authentication method :" + authMethod.name());
    }
    return authentication;
  }
}
