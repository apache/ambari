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

package org.apache.ambari.server.security.authorization.internal;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.google.inject.Inject;


public class InternalTokenClientFilter implements ClientRequestFilter {
  public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
  private final InternalTokenStorage tokenStorage;

  @Inject
  public InternalTokenClientFilter(InternalTokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
  }

  public void filter(ClientRequestContext clientRequestContext) throws IOException {
    clientRequestContext.getHeaders().add(INTERNAL_TOKEN_HEADER, tokenStorage.getInternalToken());
  }
}
