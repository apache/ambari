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

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.inject.Named;

@Named
public class SessionManager {

  private static final Logger logger = Logger.getLogger(SessionManager.class);

  public SessionManager() {
    logger.debug("SessionManager created");
  }

  public User processSuccessLogin() {
    boolean newSessionCreation = true;
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
    String currentLoginId = authentication.getName();
    LogSearchContext context = LogSearchContext.getContext();
    User user = context.getUser();
    if (user != null) {
      if (validateUser(user, currentLoginId)) {
        newSessionCreation = false;
      }
    }
    //
    if (newSessionCreation) {
      user = new User();
      user.setUsername(currentLoginId);
      if (details != null) {
        logger.info("Login Success: loginId=" + currentLoginId + ", sessionId=" + details.getSessionId()
          + ", requestId=" + details.getRemoteAddress());
      } else {
        logger.info("Login Success: loginId=" + currentLoginId + ", msaSessionId=" + ", details is null");
      }

    }

    return user;
  }

  private boolean validateUser(User user, String currentUsername) {
    if (currentUsername.equalsIgnoreCase(user.getUsername())) {
      return true;
    } else {
      logger.info("loginId doesn't match loginId from HTTPSession. Will create new session. loginId="
        + currentUsername + ", user=" + user, new Exception());
      return false;
    }
  }

}
