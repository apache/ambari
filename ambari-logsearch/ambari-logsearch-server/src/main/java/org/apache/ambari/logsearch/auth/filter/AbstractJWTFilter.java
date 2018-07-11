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
package org.apache.ambari.logsearch.auth.filter;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.apache.ambari.logsearch.auth.model.JWTAuthenticationToken;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractJWTFilter extends AbstractAuthenticationProcessingFilter {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractJWTFilter.class);

  private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
  private static final String PEM_FOOTER = "\n-----END CERTIFICATE-----";
  private static final String PROXY_LOGSEARCH_URL_PATH = "/logsearch";

  protected AbstractJWTFilter(RequestMatcher requestMatcher) {
    super(requestMatcher);
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
    if (StringUtils.isEmpty(getProvidedUrl())) {
      throw new BadCredentialsException("Authentication provider URL must not be null or empty.");
    }
    if (StringUtils.isEmpty(getPublicKey())) {
      throw new BadCredentialsException("Public key for signature validation must be provisioned.");
    }

    try {
      Claims claims = Jwts
        .parser()
        .setSigningKey(parseRSAPublicKey(getPublicKey()))
        .parseClaimsJws(getJWTFromCookie(request))
        .getBody();

      String userName  = claims.getSubject();
      LOG.info("USERNAME: " + userName);
      LOG.info("URL = " + request.getRequestURL());
      if (StringUtils.isNotEmpty(claims.getAudience()) && !getAudiences().contains(claims.getAudience())) {
        throw new IllegalArgumentException(String.format("Audience validation failed. (Not found: %s)", claims.getAudience()));
      }
      Authentication authentication = new JWTAuthenticationToken(userName, getPublicKey(), getAuthorities());
      authentication.setAuthenticated(true);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      return authentication;
    } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
      LOG.info("URL = " + request.getRequestURL());
      LOG.warn("Error during JWT authentication: {}", e.getMessage());
      throw new BadCredentialsException(e.getMessage(), e);
    }
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!isAuthJwtEnabled() || isAuthenticated(authentication)) {
      chain.doFilter(req, res);
      return;
    }
    super.doFilter(req, res, chain);
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
    super.successfulAuthentication(request, response, chain, authResult);
    String ajaxRequestHeader = request.getHeader("X-Requested-With");
    if (isWebUserAgent(request.getHeader("User-Agent")) && !"XMLHttpRequest".equals(ajaxRequestHeader)) {
      chain.doFilter(request, response);
      //response.sendRedirect(createForwardableURL(request) + getOriginalQueryString(request));
    }
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
    super.unsuccessfulAuthentication(request, response, failed);
    String ajaxRequestHeader = request.getHeader("X-Requested-With");
    String loginUrl = constructLoginURL(request);
    if (loginUrl.endsWith("?doAs=anonymous")) { // HACK! - use proper solution, investigate which filter changes ? to &
      loginUrl = StringUtils.removeEnd(loginUrl, "?doAs=anonymous");
    }
    if (!isWebUserAgent(request.getHeader("User-Agent")) || "XMLHttpRequest".equals(ajaxRequestHeader)) {
      Map<String, String> mapObj = new HashMap<>();
      mapObj.put("knoxssoredirectURL", URLEncoder.encode(loginUrl, "UTF-8"));
      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,  new Gson().toJson(mapObj));
    } else {
      response.sendRedirect(loginUrl);
    }
  }

  private String getJWTFromCookie(HttpServletRequest req) {
    String serializedJWT = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (getCookieName().equals(cookie.getName())) {
          LOG.info(getCookieName() + " cookie has been found and is being processed");
          serializedJWT = cookie.getValue();
          break;
        }
      }
    }
    return serializedJWT;
  }

  private boolean isWebUserAgent(String userAgent) {
    boolean isWeb = false;
    List<String> userAgentList = getUserAgentList();
    if (userAgentList != null && userAgentList.size() > 0) {
      for (String ua : userAgentList) {
        if (StringUtils.startsWithIgnoreCase(userAgent, ua)) {
          isWeb = true;
          break;
        }
      }
    }
    return isWeb;
  }

  private RSAPublicKey parseRSAPublicKey(String pem) throws ServletException {
    String fullPem = PEM_HEADER + pem + PEM_FOOTER;
    try {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      ByteArrayInputStream is = new ByteArrayInputStream(fullPem.getBytes("UTF8"));

      X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
      return (RSAPublicKey) cer.getPublicKey();
    } catch (CertificateException ce) {
      String message;
      if (pem.startsWith(PEM_HEADER)) {
        message = "CertificateException - be sure not to include PEM header "
          + "and footer in the PEM configuration element.";
      } else {
        message = "CertificateException - PEM may be corrupt";
      }
      throw new ServletException(message, ce);
    } catch (UnsupportedEncodingException uee) {
      throw new ServletException(uee);
    }
  }

  private String constructLoginURL(HttpServletRequest request) {
    String delimiter = "?";
    if (getProvidedUrl().contains("?")) {
      delimiter = "&";
    }
    return getProvidedUrl() + delimiter
      + getOriginalUrlQueryParam() + "="
      + createForwardableURL(request) + getOriginalQueryString(request);
  }

  private String createForwardableURL(HttpServletRequest request) {
    String xForwardedProto = request.getHeader("x-forwarded-proto");
    String xForwardedHost = request.getHeader("x-forwarded-host");
    String xForwardedContext = request.getHeader("x-forwarded-context");
    if (StringUtils.isNotBlank(xForwardedProto)
      && StringUtils.isNotBlank(xForwardedHost)
      && StringUtils.isNotBlank(xForwardedContext)) {
      try {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(xForwardedProto)
          .setHost(xForwardedHost)
          .setPath(xForwardedContext + PROXY_LOGSEARCH_URL_PATH + request.getRequestURI());

        return builder.build().toString();
      } catch (URISyntaxException ue) {
        LOG.error("URISyntaxException while build xforward url ", ue);
        return request.getRequestURL().toString();
      }
    } else {
      return request.getRequestURL().toString();
    }
  }

  private String getOriginalQueryString(HttpServletRequest request) {
    String originalQueryString = request.getQueryString();
    return (originalQueryString == null) ? "" : "?" + originalQueryString;
  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
  }

  protected abstract String getPublicKey();

  protected abstract String getProvidedUrl();

  protected abstract boolean isAuthJwtEnabled();

  protected abstract String getCookieName();

  protected abstract String getOriginalUrlQueryParam();

  protected abstract List<String> getAudiences();

  protected abstract Collection<? extends GrantedAuthority> getAuthorities();

  protected abstract List<String> getUserAgentList();

}
