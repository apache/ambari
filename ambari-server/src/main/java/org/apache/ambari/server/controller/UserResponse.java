/**
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
package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a user maintenance request.
 */
public class UserResponse {
  
  private Set<String> roles = Collections.emptySet();
  private String userName;
  
  public UserResponse(String name) {
    this.userName = name;
  }
  
  public String getUsername() {
    return userName;
  }
  
  public Set<String> getRoles() {
    return roles;
  }
  
  public void setRoles(Set<String> userRoles) {
    roles = userRoles;
  }
  
}
