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

import org.springframework.security.core.AuthenticationException;

/**
 * AuthenticationJwtUserNotFoundException is an AuthenticationException implementation to be thrown
 * when the user specified in a JTW token is not found in the Ambari user database.
 */
public class AuthenticationJwtUserNotFoundException extends AuthenticationException {
  private final String username;

  public AuthenticationJwtUserNotFoundException(String username, String message) {
    super(message);
    this.username = username;
  }

  public AuthenticationJwtUserNotFoundException(String username, String message, Throwable throwable) {
    super(message, throwable);
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
