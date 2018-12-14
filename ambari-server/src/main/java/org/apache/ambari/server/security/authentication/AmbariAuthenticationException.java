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

package org.apache.ambari.server.security.authentication;

import org.springframework.security.core.AuthenticationException;

/**
 * AmbariAuthenticationException is an AuthenticationException implementation to be thrown
 * when the user fails to authenticate with Ambari.
 */
public class AmbariAuthenticationException extends AuthenticationException {
  private final String username;
  private final String proxyUserName;

  /**
   * A boolean value indicating whether the faulire was due to invalid credentials (<code>true</code>) or not (<code>false</code>)
   * <p>
   * An invalid credential failure will count towards a user's authentication failure count.
   */
  private final boolean credentialFailure;

  public AmbariAuthenticationException(String username, String message, boolean credentialFailure) {
    super(message);
    this.username = username;
    this.proxyUserName = null;
    this.credentialFailure = credentialFailure;
  }

  public AmbariAuthenticationException(String username, String message,  boolean credentialFailure, Throwable throwable) {
    super(message, throwable);
    this.username = username;
    this.proxyUserName = null;
    this.credentialFailure = credentialFailure;
  }

  private AmbariAuthenticationException(String username, String proxyUserName, String message, boolean credentialFailure, Throwable throwable) {
    super(message, throwable);
    this.username = username;
    this.proxyUserName = proxyUserName;
    this.credentialFailure = credentialFailure;
  }

  public String getUsername() {
    return username;
  }

  public boolean isCredentialFailure() {
    return credentialFailure;
  }

  public String getProxyUserName() {
    return proxyUserName;
  }


  public static final class AmbariAuthenticationExceptionBuilder {
    private String username;
    private String proxyUserName;
    private boolean credentialFailure;
    private String message;
    private Throwable throwable;

    private AmbariAuthenticationExceptionBuilder() {
    }

    public static AmbariAuthenticationExceptionBuilder anAmbariAuthenticationException() {
      return new AmbariAuthenticationExceptionBuilder();
    }

    public AmbariAuthenticationExceptionBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public AmbariAuthenticationExceptionBuilder withProxyUserName(String proxyUserName) {
      this.proxyUserName = proxyUserName;
      return this;
    }

    public AmbariAuthenticationExceptionBuilder withCredentialFailure(boolean credentialFailure) {
      this.credentialFailure = credentialFailure;
      return this;
    }

    public AmbariAuthenticationExceptionBuilder withMessage(String message) {
      this.message = message;
      return this;
    }

    public AmbariAuthenticationExceptionBuilder withTrowable(Throwable throwable) {
      this.throwable = throwable;
      return this;
    }

    public AmbariAuthenticationException build() {
      return new AmbariAuthenticationException(username, proxyUserName, message, credentialFailure, throwable);
    }
  }
}
