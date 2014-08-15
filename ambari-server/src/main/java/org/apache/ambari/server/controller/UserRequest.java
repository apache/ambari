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

/**
 * Represents a user maintenance request.
 */
public class UserRequest {
  private String userName;
  private String password;
  private String oldPassword;
  private Boolean active;
  private Boolean admin;

  public UserRequest(String name) {
    this.userName = name;
  }

  public String getUsername() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String userPass) {
    password = userPass;
  }

  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldUserPass) {
    oldPassword = oldUserPass;
  }

  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("User, username=" + userName);
    return sb.toString();
  }

}
