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
package org.apache.ambari.server.actionmanager;

import java.util.Map;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;

/**
 * This class encapsulates all the information for an action
 * on a host for a particular role. This class will be used to schedule, persist and track
 * an action.
 */
public class HostRoleCommand {
  private final Role role;
  private Map<String, String> params = null;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private final RoleCommand cmd;
  private long startTime;
  private long expiryTime;
  private final String host;

  public HostRoleCommand(String host, Role role, RoleCommand cmd) {
    this.host = host;
    this.role = role;
    this.cmd = cmd;
  }

  public Role getRole() {
    return role;
  }

  public HostRoleStatus getStatus() {
    return status;
  }
  
  public long getStartTime() {
    return startTime;
  }
  
  public long getExpiryTime() {
    return expiryTime;
  }
  
  public void setExpiryTime(long t) {
    expiryTime = t;
  }

  public String getHostName() {
    return this.host;
  }
}
