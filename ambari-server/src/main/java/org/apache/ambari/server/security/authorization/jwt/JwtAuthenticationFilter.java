/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization.jwt;

import com.google.inject.Inject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Filter is used to validate JWT token and authenticate user.
 * It is also responsive for creating user in local Ambari database for further management
 */
public class JwtAuthenticationFilter implements Filter {
  Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtAuthenticationProperties jwtProperties;

  private String originalUrlQueryParam = "originalUrl";
  private String authenticationProviderUrl = null;
  private RSAPublicKey publicKey = null;
  private List<String> audiences = null;
  private String cookieName = "hadoop-jwt";

  private boolean ignoreFailure = true;
  private AuthenticationEntryPoint entryPoint;
  private Users users;

  @Inject
  public JwtAuthenticationFilter(Configuration configuration, AuthenticationEntryPoint entryPoint, Users users) {
    this.entryPoint = entryPoint;
    this.users = users;
    jwtProperties = configuration.getJwtProperties();
    loadJwtProperties();
  }

  public JwtAuthenticationFilter(JwtAuthenticationProperties jwtProperties, AuthenticationEntryPoint entryPoint,
                                 Users users) {
    this.jwtProperties = jwtProperties;
    this.entryPoint = entryPoint;
    this.users = users;
    loadJwtProperties();
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    if(jwtProperties == null){
      //disable filter if not configured
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

    String serializedJWT = getJWTFromCookie(httpServletRequest);
    if (serializedJWT != null && isAuthenticationRequired(serializedJWT)) {
      SignedJWT jwtToken;
      try {
        jwtToken = SignedJWT.parse(serializedJWT);

        boolean valid = validateToken(jwtToken);

        if (valid) {
          String userName = jwtToken.getJWTClaimsSet().getSubject();
          User user = users.getUser(userName, UserType.JWT);
          if (user == null) {

            //TODO this is temporary check for conflicts, until /users API will change to use user_id instead of name as PK
            User existingUser = users.getAnyUser(userName);
            if (existingUser != null && existingUser.getUserType() != UserType.JWT) {

              LOG.error("Access for JWT user [{}] restricted. Detected conflict with local user ", userName);

              // directly send HTTP status 500 to avoid redirect loop, as jwt token is already confirmed to be valid
              httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Cannot create JWT user: conflict detected");

              //interrupt filter chain
              return;
            }

            // create user in local database on first login, usually we cannot fetch all users
            // from external authentication provider (as we do during ldap-sync process)
            users.createUser(userName, null, UserType.JWT, true, false);
            user = users.getUser(userName, UserType.JWT);
          }

          Collection<AmbariGrantedAuthority> userAuthorities =
                  users.getUserAuthorities(user.getUserName(), user.getUserType());

          JwtAuthentication authentication = new JwtAuthentication(serializedJWT, user, userAuthorities);
          authentication.setAuthenticated(true);

          SecurityContextHolder.getContext().setAuthentication(authentication);


        } else {
          //clear security context if authentication was required, but failed
          SecurityContextHolder.clearContext();

          LOG.warn("JWT authentication failed");
          if (ignoreFailure) {
            filterChain.doFilter(servletRequest, servletResponse);
          } else {
            //used to indicate authentication failure, not used here as we have more than one filter
            entryPoint.commence(httpServletRequest, httpServletResponse, new BadCredentialsException("Invalid JWT " +
                    "token"));
          }
        }


      } catch (ParseException e) {
        LOG.warn("Unable to parse the JWT token", e);
      }
    } else {
      LOG.trace("No JWT cookie found, do nothing");
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  private void loadJwtProperties() {
    if (jwtProperties != null) {
      authenticationProviderUrl = jwtProperties.getAuthenticationProviderUrl();
      publicKey = jwtProperties.getPublicKey();
      audiences = jwtProperties.getAudiences();
      cookieName = jwtProperties.getCookieName();
      originalUrlQueryParam = jwtProperties.getOriginalUrlQueryParam();
    }
  }

  /**
   * Do not try to validate JWT if user already authenticated via other provider
   * @return true, if JWT validation required
   */
  private boolean isAuthenticationRequired(String token) {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

    //authenticate if no auth
    if(existingAuth == null || !existingAuth.isAuthenticated() ){
      return true;
    }

    //revalidate if token was changed
    if(existingAuth instanceof JwtAuthentication && !StringUtils.equals(token, (String) existingAuth.getCredentials())){
      return true;
    }

    //always try to authenticate in case of anonymous user
    if (existingAuth instanceof AnonymousAuthenticationToken) {
      return true;
    }

    return false;
  }

  /**
   * Encapsulate the acquisition of the JWT token from HTTP cookies within the
   * request.
   *
   * @param req servlet request to get the JWT token from
   * @return serialized JWT token
   */
  protected String getJWTFromCookie(HttpServletRequest req) {
    String serializedJWT = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          LOG.info(cookieName
              + " cookie has been found and is being processed");
          serializedJWT = cookie.getValue();
          break;
        }
      }
    }
    return serializedJWT;
  }
  /**
   * Create the URL to be used for authentication of the user in the absence of
   * a JWT token within the incoming request.
   *
   * @param request for getting the original request URL
   * @return url to use as login url for redirect
   */
  protected String constructLoginURL(HttpServletRequest request) {
    String delimiter = "?";
    if (authenticationProviderUrl.contains("?")) {
      delimiter = "&";
    }
    String loginURL = authenticationProviderUrl + delimiter
        + originalUrlQueryParam + "="
        + request.getRequestURL().toString();
    return loginURL;
  }

  /**
   * This method provides a single method for validating the JWT for use in
   * request processing. It provides for the override of specific aspects of
   * this implementation through submethods used within but also allows for the
   * override of the entire token validation algorithm.
   *
   * @param jwtToken the token to validate
   * @return true if valid
   */
  protected boolean validateToken(SignedJWT jwtToken) {
    boolean sigValid = validateSignature(jwtToken);
    if (!sigValid) {
      LOG.warn("Signature could not be verified");
    }
    boolean audValid = validateAudiences(jwtToken);
    if (!audValid) {
      LOG.warn("Audience validation failed.");
    }
    boolean expValid = validateExpiration(jwtToken);
    if (!expValid) {
      LOG.info("Expiration validation failed.");
    }

    return sigValid && audValid && expValid;
  }

  /**
   * Verify the signature of the JWT token in this method. This method depends
   * on the public key that was established during init based upon the
   * provisioned public key. Override this method in subclasses in order to
   * customize the signature verification behavior.
   *
   * @param jwtToken the token that contains the signature to be validated
   * @return valid true if signature verifies successfully; false otherwise
   */
  protected boolean validateSignature(SignedJWT jwtToken) {
    boolean valid = false;
    if (JWSObject.State.SIGNED == jwtToken.getState()) {
      LOG.debug("JWT token is in a SIGNED state");
      if (jwtToken.getSignature() != null) {
        LOG.debug("JWT token signature is not null");
        try {
          JWSVerifier verifier = new RSASSAVerifier(publicKey);
          if (jwtToken.verify(verifier)) {
            valid = true;
            LOG.debug("JWT token has been successfully verified");
          } else {
            LOG.warn("JWT signature verification failed.");
          }
        } catch (JOSEException je) {
          LOG.warn("Error while validating signature", je);
        }
      }
    }
    return valid;
  }

  /**
   * Validate whether any of the accepted audience claims is present in the
   * issued token claims list for audience. Override this method in subclasses
   * in order to customize the audience validation behavior.
   *
   * @param jwtToken
   *          the JWT token where the allowed audiences will be found
   * @return true if an expected audience is present, otherwise false
   */
  protected boolean validateAudiences(SignedJWT jwtToken) {
    boolean valid = false;
    try {
      List<String> tokenAudienceList = jwtToken.getJWTClaimsSet()
          .getAudience();
      // if there were no expected audiences configured then just
      // consider any audience acceptable
      if (audiences == null) {
        valid = true;
      } else {
        // if any of the configured audiences is found then consider it
        // acceptable
        if (tokenAudienceList == null) {
          LOG.warn("JWT token has no audiences, validation failed.");
          return false;
        }
        for (String aud : tokenAudienceList) {
          if (audiences.contains(aud)) {
            LOG.debug("JWT token audience has been successfully validated");
            valid = true;
            break;
          }
        }
        if (!valid) {
          LOG.warn("JWT audience validation failed.");
        }
      }
    } catch (ParseException pe) {
      LOG.warn("Unable to parse the JWT token.", pe);
    }
    return valid;
  }

  /**
   * Validate that the expiration time of the JWT token has not been violated.
   * If it has then throw an AuthenticationException. Override this method in
   * subclasses in order to customize the expiration validation behavior.
   *
   * @param jwtToken the token that contains the expiration date to validate
   * @return valid true if the token has not expired; false otherwise
   */
  protected boolean validateExpiration(SignedJWT jwtToken) {
    boolean valid = false;
    try {
      Date expires = jwtToken.getJWTClaimsSet().getExpirationTime();
      if (expires == null || new Date().before(expires)) {
        LOG.debug("JWT token expiration date has been "
            + "successfully validated");
        valid = true;
      } else {
        LOG.warn("JWT expiration date validation failed.");
      }
    } catch (ParseException pe) {
      LOG.warn("JWT expiration date validation failed.", pe);
    }
    return valid;
  }

  @Override
  public void destroy() {

  }
}
