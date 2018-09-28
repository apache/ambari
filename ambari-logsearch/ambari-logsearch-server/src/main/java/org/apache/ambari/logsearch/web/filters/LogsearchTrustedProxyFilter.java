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

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.dao.RoleDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

/**
 * Filter servlet to handle trusted proxy authentication.
 * It is disabled by default (see: {@link AuthPropsConfig#isTrustedProxy()}) <br/>
 * There are 4 main configuration properties of this filter (allow authentication only if these are matches with the request details): <br/>
 * - {@link AuthPropsConfig#getProxyUsers()} - Proxy users <br/>
 * - {@link AuthPropsConfig#getProxyUserGroups()} - Proxy groups <br/>
 * - {@link AuthPropsConfig#getProxyUserHosts()} - Proxy hosts <br/>
 * - {@link AuthPropsConfig#getProxyIp()} - Proxy server IPs<br/>
 */
public class LogsearchTrustedProxyFilter extends AbstractAuthenticationProcessingFilter {

  private static final Logger LOG = LoggerFactory.getLogger(LogsearchTrustedProxyFilter.class);

  private static final String TRUSTED_PROXY_KNOX_HEADER = "X-Forwarded-For";

  private AuthPropsConfig authPropsConfig;

  public LogsearchTrustedProxyFilter(RequestMatcher requestMatcher, AuthPropsConfig authPropsConfig) {
    super(requestMatcher);
    this.authPropsConfig = authPropsConfig;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
    String doAsUserName = request.getParameter("doAs");
    final List<GrantedAuthority> authorities = RoleDao.createDefaultAuthorities();
    final UserDetails principal = new User(doAsUserName, "", authorities);
    final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, "", authorities);
    WebAuthenticationDetails webDetails = new WebAuthenticationDetails(request);
    ((AbstractAuthenticationToken) finalAuthentication).setDetails(webDetails);
    SecurityContextHolder.getContext().setAuthentication(finalAuthentication);
    LOG.info("Logged into Log Search User as doAsUser = {}", doAsUserName);
    return finalAuthentication;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean skip = true;
    if (authPropsConfig.isTrustedProxy() && !isAuthenticated(authentication) ) {
      String doAsUserName = req.getParameter("doAs");
      String remoteAddr = req.getRemoteAddr();
      if (StringUtils.isNotEmpty(doAsUserName) && isTrustedProxySever(remoteAddr)
        && isTrustedHost(getXForwardHeader((HttpServletRequest) req))) {
        List<GrantedAuthority> grantedAuths = RoleDao.createDefaultAuthorities();
        if (!(isTrustedProxyUser(doAsUserName) || isTrustedProxyUserGroup(grantedAuths))) {
          skip = false;
        }
      }
    }
    if (skip) {
      chain.doFilter(req, res);
      return;
    }
    super.doFilter(req, res, chain);
  }

  private boolean isTrustedProxySever(String requestHosts) {
    if (authPropsConfig.getProxyIp() == null || requestHosts == null) {
      return false;
    }
    final List<String> proxyServers = authPropsConfig.getProxyIp();
    return (proxyServers.size() == 1 && proxyServers.contains("*")) || authPropsConfig.getProxyIp().contains(requestHosts);
  }

  private boolean isTrustedHost(String requestHosts) {
    if (requestHosts == null) {
      return false;
    }
    List<String> trustedProxyHosts = authPropsConfig.getProxyUserHosts();
    return (trustedProxyHosts.size() == 1 && trustedProxyHosts.contains("*")) || trustedProxyHosts.contains(requestHosts);
  }

  private boolean isTrustedProxyUser(String doAsUser) {
    if (doAsUser == null) {
      return false;
    }
    List<String> trustedProxyUsers = authPropsConfig.getProxyUsers();
    return (trustedProxyUsers.size() == 1 && trustedProxyUsers.contains("*")) || trustedProxyUsers.contains(doAsUser);

  }

  private boolean isTrustedProxyUserGroup(List<GrantedAuthority> proxyUserGroup) {
    if (proxyUserGroup == null) {
      return false;
    }
    List<String> trustedProxyGroups = authPropsConfig.getProxyUserGroups();
    if (trustedProxyGroups.size() == 1 && trustedProxyGroups.contains("*")) {
      return true;
    } else {
      for (GrantedAuthority group : proxyUserGroup) {
        if (trustedProxyGroups.contains(group.getAuthority())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
  }

  private String getXForwardHeader(HttpServletRequest httpRequest) {
    Enumeration<String> names = httpRequest.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      Enumeration<String> values = httpRequest.getHeaders(name);
      String value = "";
      if (values != null) {
        while (values.hasMoreElements()) {
          value = values.nextElement();
          if (StringUtils.isNotBlank(value)) {
            break;
          }
        }
      }
      if (StringUtils.trimToNull(name) != null
        && StringUtils.trimToNull(value) != null) {
        if (name.equalsIgnoreCase(TRUSTED_PROXY_KNOX_HEADER)) {
          return value;
        }
      }
    }
    return "";
  }
}
