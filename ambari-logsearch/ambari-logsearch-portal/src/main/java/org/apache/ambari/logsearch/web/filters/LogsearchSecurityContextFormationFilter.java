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
package org.apache.ambari.logsearch.web.filters;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.manager.SessionManager;
import org.apache.ambari.logsearch.util.CommonUtil;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class LogsearchSecurityContextFormationFilter extends GenericFilterBean {

  static Logger logger = Logger.getLogger(LogsearchSecurityContextFormationFilter.class);

  public static final String LOGSEARCH_SC_SESSION_KEY = "LOGSEARCH_SECURITY_CONTEXT";
  public static final String USER_AGENT = "User-Agent";

  @Inject
  SessionManager sessionManager;

  public LogsearchSecurityContextFormationFilter() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
    ServletException {

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();

      if (auth instanceof AnonymousAuthenticationToken) {
        // ignore
      } else {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession httpSession = httpRequest.getSession(false);
        Cookie[] cookieList = httpRequest.getCookies();
        String msaCookie = null;
        for (int i = 0; cookieList != null && i < cookieList.length; i++) {
          if (cookieList[i].getName().equalsIgnoreCase("msa")) {
            msaCookie = cookieList[i].getValue();
          }
        }
        if (msaCookie == null) {
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          msaCookie = CommonUtil.genGUI();
          Cookie cookie = new Cookie("msa", msaCookie);
          // TODO: Need to revisit this
          cookie.setMaxAge(Integer.MAX_VALUE);
          httpResponse.addCookie(cookie);
        }
        // [1]get the context from session
        LogSearchContext context = (LogSearchContext) httpSession
          .getAttribute(LOGSEARCH_SC_SESSION_KEY);
        if (context == null) {
          context = new LogSearchContext();
          httpSession.setAttribute(LOGSEARCH_SC_SESSION_KEY, context);
        }
        LogSearchContext.setContext(context);
        User user = sessionManager.processSuccessLogin();
        context.setUser(user);
      }
      chain.doFilter(request, response);

    } finally {
      // [4]remove context from thread-local
      LogSearchContext.resetContext();
    }
  }
}
