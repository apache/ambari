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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.server.KerberosAuthenticationHandler;
import org.apache.hadoop.security.authentication.server.PseudoAuthenticationHandler;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

public class LogsearchKRBAuthenticationFilter extends LogsearchKrbFilter {
  private static final Logger logger = LoggerFactory.getLogger(LogsearchKRBAuthenticationFilter.class);

  private static final String NAME_RULES = "hadoop.security.auth_to_local";
  private static final String TOKEN_VALID = "logsearch.admin.kerberos.token.valid.seconds";
  private static final String COOKIE_DOMAIN = "logsearch.admin.kerberos.cookie.domain";
  private static final String COOKIE_PATH = "logsearch.admin.kerberos.cookie.path";
  private static final String PRINCIPAL = "logsearch.spnego.kerberos.principal";
  private static final String KEYTAB = "logsearch.spnego.kerberos.keytab";
  private static final String HOST_NAME = "logsearch.spnego.kerberos.host";
  private static final String KERBEROS_ENABLE="logsearch.spnego.kerberos.enable";

  private static final String NAME_RULES_PARAM = "kerberos.name.rules";
  private static final String TOKEN_VALID_PARAM = "token.validity";
  private static final String COOKIE_DOMAIN_PARAM = "cookie.domain";
  private static final String COOKIE_PATH_PARAM = "cookie.path";
  private static final String PRINCIPAL_PARAM = "kerberos.principal";
  private static final String KEYTAB_PARAM = "kerberos.keytab";
  private static final String AUTH_TYPE = "type";
  private static final String AUTH_COOKIE_NAME = "hadoop.auth";
  private static final String DEFAULT_USER_ROLE = "ROLE_USER";

  private static final NoServletContext NO_SERVLET_CONTEXT = new NoServletContext();
  private static final Pattern usernamePattern = Pattern.compile("(?<=u=)(.*?)(?=&)|(?<=u=)(.*)");
  
  private String authType = PseudoAuthenticationHandler.TYPE;
  private static boolean spnegoEnable = false;

  public LogsearchKRBAuthenticationFilter() {
    try {
      isSpnegoEnable();
      init(null);
    } catch (ServletException e) {
      logger.error("Error while initializing Filter : " + e.getMessage());
    }
  }

  @Override
  public void init(FilterConfig conf) throws ServletException {
    final FilterConfig globalConf = conf;
    String hostName = PropertiesHelper.getProperty(HOST_NAME, "localhost");
    final Map<String, String> params = new HashMap<String, String>();
    if (spnegoEnable) {
      authType = KerberosAuthenticationHandler.TYPE;
    }
    params.put(AUTH_TYPE,authType);
    params.put(NAME_RULES_PARAM,PropertiesHelper.getProperty(NAME_RULES, "DEFAULT"));
    params.put(TOKEN_VALID_PARAM, PropertiesHelper.getProperty(TOKEN_VALID, "30"));
    params.put(COOKIE_DOMAIN_PARAM, PropertiesHelper.getProperty(COOKIE_DOMAIN, hostName));
    params.put(COOKIE_PATH_PARAM, PropertiesHelper.getProperty(COOKIE_PATH, "/"));
    params.put(PRINCIPAL_PARAM,PropertiesHelper.getProperty(PRINCIPAL,""));
    params.put(KEYTAB_PARAM,PropertiesHelper.getProperty(KEYTAB,""));
    FilterConfig myConf = new FilterConfig() {
      @Override
      public ServletContext getServletContext() {
        if (globalConf != null) {
          return globalConf.getServletContext();
        } else {
          return NO_SERVLET_CONTEXT;
        }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Enumeration<String> getInitParameterNames() {
        return new IteratorEnumeration(params.keySet().iterator());
      }

      @Override
      public String getInitParameter(String param) {
        return params.get(param);
      }

      @Override
      public String getFilterName() {
        return "KerberosFilter";
      }
    };
    super.init(myConf);
  }

  @Override
  protected void doFilter(FilterChain filterChain, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    logger.debug("LogsearchKRBAuthenticationFilter private filter");
    String userName = getUsernameFromResponse(response);
    if (!StringUtils.isEmpty(userName)) {
      Authentication existingAuth = SecurityContextHolder.getContext()
          .getAuthentication();
      if (existingAuth == null || !existingAuth.isAuthenticated()) {
        // --------------------------- To Create Logsearch Session--------------------------------------
        // if we get the userName from the token then log into Logsearch using the same user
        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(DEFAULT_USER_ROLE));
        final UserDetails principal = new User(userName, "", grantedAuths);
        final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
            principal, "", grantedAuths);
        WebAuthenticationDetails webDetails = new WebAuthenticationDetails(
            request);
        ((AbstractAuthenticationToken) finalAuthentication)
            .setDetails(webDetails);
        Authentication authentication = this
            .authenticate(finalAuthentication);
        authentication = getGrantedAuthority(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT",
            SecurityContextHolder.getContext());
        request.setAttribute("spnegoEnabled", true);
        logger.info("Logged into Logsearch as = " + userName);
        filterChain.doFilter(request, response);
      } else {
        try {
          super.doFilter(filterChain, request, response);
        } catch (Exception e) {
          logger.error("Error LogsearchKRBAuthenticationFilter : " + e.getMessage());
        }
      }
    } else {
      filterChain.doFilter(request, response);
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    logger.debug("LogsearchKRBAuthenticationFilter public filter path >>>>" +httpRequest.getPathInfo());
    SecurityContextImpl securityContextImpl=(SecurityContextImpl) httpRequest.getSession(true).getAttribute("SPRING_SECURITY_CONTEXT");
    Authentication existingAuth = null;
    if(securityContextImpl!=null){
      existingAuth= securityContextImpl.getAuthentication();
    }
    if (!isLoginRequest(httpRequest) && spnegoEnable
        && (existingAuth == null || !existingAuth.isAuthenticated())) {
      KerberosName.setRules(PropertiesHelper.getProperty(NAME_RULES, "DEFAULT"));
      String userName = getUsernameFromRequest(httpRequest);
      if ((existingAuth == null || !existingAuth.isAuthenticated())
          && (!StringUtils.isEmpty(userName))) {
        // --------------------------- To Create Logsearch Session--------------------------------------
        // if we get the userName from the token then log into logsearch using the same user
        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(DEFAULT_USER_ROLE));
        final UserDetails principal = new User(userName, "", grantedAuths);
        final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
            principal, "", grantedAuths);
        WebAuthenticationDetails webDetails = new WebAuthenticationDetails(
            httpRequest);
        ((AbstractAuthenticationToken) finalAuthentication)
            .setDetails(webDetails);
        Authentication authentication = this
            .authenticate(finalAuthentication);
        authentication = getGrantedAuthority(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("spnegoEnabled", true);
        logger.info("Logged into Logsearch as = " + userName);
      }else {
        try {
          super.doFilter(request, response, filterChain);
        } catch (Exception e) {
          logger.error("Error LogsearchKRBAuthenticationFilter : " + e.getMessage());
        }
      }
    } else {
      filterChain.doFilter(request, response);
    }
  }

  private void isSpnegoEnable() {
    spnegoEnable = PropertiesHelper.getBooleanProperty(KERBEROS_ENABLE, false);
    if (spnegoEnable) {
      spnegoEnable = false;
      String keytab = PropertiesHelper.getProperty(KEYTAB);
      String principal = PropertiesHelper.getProperty(PRINCIPAL);
      String hostname = PropertiesHelper.getProperty(HOST_NAME);
      if (!StringUtils.isEmpty(keytab) && !StringUtils.isEmpty(principal)
          && !StringUtils.isEmpty(hostname)) {
        spnegoEnable = true;
      }
    }
  }

  private Authentication getGrantedAuthority(Authentication authentication) {
    UsernamePasswordAuthenticationToken result = null;
    if (authentication != null && authentication.isAuthenticated()) {
      final List<GrantedAuthority> grantedAuths = getAuthorities(authentication
          .getName().toString());
      final UserDetails userDetails = new User(authentication.getName()
          .toString(), authentication.getCredentials().toString(), grantedAuths);
      result = new UsernamePasswordAuthenticationToken(userDetails,
          authentication.getCredentials(), grantedAuths);
      result.setDetails(authentication.getDetails());
      return result;
    }
    return authentication;
  }

  private List<GrantedAuthority> getAuthorities(String username) {
    final List<GrantedAuthority> grantedAuths = new ArrayList<>();
    grantedAuths.add(new SimpleGrantedAuthority(DEFAULT_USER_ROLE));
    return grantedAuths;
  }
  
  private Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();
    username = StringEscapeUtils.unescapeHtml(username);
    if (StringUtils.isEmpty(username)) {
      throw new BadCredentialsException("Username can't be null or empty.");
    }
    org.apache.ambari.logsearch.web.model.User user = new org.apache.ambari.logsearch.web.model.User();
    user.setUsername(username);
    authentication = new UsernamePasswordAuthenticationToken(username,
        password, getAuthorities(username));
    return authentication;
  }
  
  private String getUsernameFromRequest(HttpServletRequest httpRequest) {
    String userName = null;
    Cookie[] cookie = httpRequest.getCookies();
    if (cookie != null) {
      for (Cookie c : cookie) {
        if (c.getName().equalsIgnoreCase(AUTH_COOKIE_NAME)) {
          String cookieStr = c.getName() + "=" + c.getValue();
          Matcher m = usernamePattern.matcher(cookieStr);
          if (m.find()) {
            userName = m.group(1);
          }
        }
      }
    }
    logger.debug("kerberos username  from  request >>>>>>>>" + userName);
    return userName;
  }

  private String getUsernameFromResponse(HttpServletResponse response) {
    String userName = null;
    boolean checkCookie = response.containsHeader("Set-Cookie");
    if (checkCookie) {
      Collection<String> cookiesCollection = response.getHeaders("Set-Cookie");
      if (cookiesCollection != null) {
        Iterator<String> iterator = cookiesCollection.iterator();
        while (iterator.hasNext()) {
          String cookie = iterator.next();
          if (!StringUtils.isEmpty(cookie)) {
            if (cookie.toLowerCase().startsWith(AUTH_COOKIE_NAME.toLowerCase())) {
              Matcher m = usernamePattern.matcher(cookie);
              if (m.find()) {
                userName = m.group(1);
              }
            }
          }
          if (!StringUtils.isEmpty(userName)) {
            break;
          }
        }
      }
    }
    logger.debug("kerberos username  from  response >>>>>>>>" + userName);
    return userName;
  }

  
  
  private boolean isLoginRequest(HttpServletRequest httpServletRequest) {
    boolean isLoginRequest = false;
    if ("POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
      String url = httpServletRequest.getRequestURI().toString();
      if ("/login".equalsIgnoreCase(url)) {
        isLoginRequest = true;
      }
    }
    return isLoginRequest;
  }
}