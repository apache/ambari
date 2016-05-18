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
package org.apache.ambari.logsearch.manager;

import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.logsearch.common.UserSessionInfo;
import org.apache.ambari.logsearch.security.context.LogsearchContextHolder;
import org.apache.ambari.logsearch.security.context.LogsearchSecurityContext;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class SessionMgr {

  static final Logger logger = Logger.getLogger(SessionMgr.class);

  public SessionMgr() {
    logger.debug("SessionManager created");
  }

  public UserSessionInfo processSuccessLogin(int authType, String userAgent) {
    return processSuccessLogin(authType, userAgent, null);
  }

  public UserSessionInfo processSuccessLogin(int authType, String userAgent, HttpServletRequest httpRequest) {
    boolean newSessionCreation = true;
    UserSessionInfo userSession = null;
    LogsearchSecurityContext context = LogsearchContextHolder.getSecurityContext();
    if (context != null) {
      userSession = context.getUserSession();
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
    String currentLoginId = authentication.getName();
    if (userSession != null) {
      if (validateUserSession(userSession, currentLoginId)) {
        newSessionCreation = false;
      }
    }
    //
    if (newSessionCreation) {
      // // Need to build the UserSession
      userSession = new UserSessionInfo();
      User user = new User();
      user.setUsername(currentLoginId);
      userSession.setUser(user);
      if (details != null) {
        logger.info("Login Success: loginId=" + currentLoginId + ", sessionId=" + details.getSessionId()
          + ", requestId=" + details.getRemoteAddress());
      } else {
        logger.info("Login Success: loginId=" + currentLoginId + ", msaSessionId=" + ", details is null");
      }

    }

    return userSession;
  }

  protected boolean validateUserSession(UserSessionInfo userSession, String currentUsername) {
    if (currentUsername.equalsIgnoreCase(userSession.getUser().getUsername())) {
      return true;
    } else {
      logger.info("loginId doesn't match loginId from HTTPSession. Will create new session. loginId="
        + currentUsername + ", userSession=" + userSession, new Exception());
      return false;
    }
  }

}
