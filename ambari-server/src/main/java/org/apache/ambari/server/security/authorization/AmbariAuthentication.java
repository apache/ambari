/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import java.security.Principal;
import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * This class is a wrapper for authentication objects to
 * provide functionality for resolving login aliases to
 * ambari user names.
 */
public final class AmbariAuthentication implements Authentication, UserIdAuthentication {
  private final Authentication authentication;
  private final Object principalOverride;
  private final Integer userId;

  public AmbariAuthentication(Authentication authentication, Integer userId) {
    this.authentication = authentication;
    this.principalOverride = getPrincipalOverride();
    this.userId = userId;
  }



  /**
   * Set by an <code>AuthenticationManager</code> to indicate the authorities that the principal has been
   * granted. Note that classes should not rely on this value as being valid unless it has been set by a trusted
   * <code>AuthenticationManager</code>.
   * <p>
   * Implementations should ensure that modifications to the returned collection
   * array do not affect the state of the Authentication object, or use an unmodifiable instance.
   * </p>
   *
   * @return the authorities granted to the principal, or an empty collection if the token has not been authenticated.
   * Never null.
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authentication.getAuthorities();
  }

  /**
   * The credentials that prove the principal is correct. This is usually a password, but could be anything
   * relevant to the <code>AuthenticationManager</code>. Callers are expected to populate the credentials.
   *
   * @return the credentials that prove the identity of the <code>Principal</code>
   */
  @Override
  public Object getCredentials() {
    return authentication.getCredentials();
  }

  /**
   * Stores additional details about the authentication request. These might be an IP address, certificate
   * serial number etc.
   *
   * @return additional details about the authentication request, or <code>null</code> if not used
   */
  @Override
  public Object getDetails() {
    return authentication.getDetails();
  }

  /**
   * The identity of the principal being authenticated. In the case of an authentication request with username and
   * password, this would be the username. Callers are expected to populate the principal for an authentication
   * request.
   * <p>
   * The <tt>AuthenticationManager</tt> implementation will often return an <tt>Authentication</tt> containing
   * richer information as the principal for use by the application. Many of the authentication providers will
   * create a {@code UserDetails} object as the principal.
   *
   * @return the <code>Principal</code> being authenticated or the authenticated principal after authentication.
   */
  @Override
  public Object getPrincipal() {
    if (principalOverride != null) {
      return principalOverride;
    }

    return authentication.getPrincipal();
  }

  /**
   * Used to indicate to {@code AbstractSecurityInterceptor} whether it should present the
   * authentication token to the <code>AuthenticationManager</code>. Typically an <code>AuthenticationManager</code>
   * (or, more often, one of its <code>AuthenticationProvider</code>s) will return an immutable authentication token
   * after successful authentication, in which case that token can safely return <code>true</code> to this method.
   * Returning <code>true</code> will improve performance, as calling the <code>AuthenticationManager</code> for
   * every request will no longer be necessary.
   * <p>
   * For security reasons, implementations of this interface should be very careful about returning
   * <code>true</code> from this method unless they are either immutable, or have some way of ensuring the properties
   * have not been changed since original creation.
   *
   * @return true if the token has been authenticated and the <code>AbstractSecurityInterceptor</code> does not need
   * to present the token to the <code>AuthenticationManager</code> again for re-authentication.
   */
  @Override
  public boolean isAuthenticated() {
    return authentication.isAuthenticated();
  }

  /**
   * See {@link #isAuthenticated()} for a full description.
   * <p>
   * Implementations should <b>always</b> allow this method to be called with a <code>false</code> parameter,
   * as this is used by various classes to specify the authentication token should not be trusted.
   * If an implementation wishes to reject an invocation with a <code>true</code> parameter (which would indicate
   * the authentication token is trusted - a potential security risk) the implementation should throw an
   * {@link IllegalArgumentException}.
   *
   * @param isAuthenticated <code>true</code> if the token should be trusted (which may result in an exception) or
   *                        <code>false</code> if the token should not be trusted
   * @throws IllegalArgumentException if an attempt to make the authentication token trusted (by passing
   *                                  <code>true</code> as the argument) is rejected due to the implementation being immutable or
   *                                  implementing its own alternative approach to {@link #isAuthenticated()}
   */
  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    authentication.setAuthenticated(isAuthenticated);
  }

  /**
   * Returns the name of this principal.
   *
   * @return the name of this principal.
   */
  @Override
  public String getName() {
    if (principalOverride != null)
    {
      if (principalOverride instanceof UserDetails) {
        return ((UserDetails) principalOverride).getUsername();
      }

      return principalOverride.toString();
    }

    return authentication.getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AmbariAuthentication that = (AmbariAuthentication) o;
    return Objects.equals(authentication, that.authentication) &&
      Objects.equals(principalOverride, that.principalOverride);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authentication, principalOverride);
  }

  /**
   * Returns a principal object that is to be used
   * to override the original principal object
   * returned by the inner {@link #authentication} object.
   *
   * <p>The purpose of overriding the origin principal is to provide
   * and object that resolves the contained user name to ambari user name in case
   * the original user name is a login alias.</p>
   *
   * @return principal override of the original one is of type {@link UserDetails},
   * if the original one is a login alias name than the user name the login alias resolves to
   * otherwise <code>null</code>
   */
  private Object getPrincipalOverride() {
    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      UserDetails user = (UserDetails)principal;
      String usernameOrig = user.getUsername();
      String username = AuthorizationHelper.resolveLoginAliasToUserName(usernameOrig);

      if (username.equals(usernameOrig))
        return null; // create override only original username is a login alias


      String userPassword = user.getPassword() != null ? user.getPassword() : "";

      principal =
        new User(
          username,
          userPassword,
          user.isEnabled(),
          user.isAccountNonExpired(),
          user.isCredentialsNonExpired(),
          user.isAccountNonLocked(),
          user.getAuthorities());
    } else if ( !(principal instanceof Principal) && principal != null ){
      String username = principal.toString();
      principal = AuthorizationHelper.resolveLoginAliasToUserName(username);
    } else {
      principal = null;
    }

    return principal;
  }

  @Override
  public Integer getUserId() {
    return userId;
  }
}
