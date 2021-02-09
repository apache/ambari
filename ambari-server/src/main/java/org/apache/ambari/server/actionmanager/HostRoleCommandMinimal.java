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
package org.apache.ambari.server.actionmanager;

import org.apache.ambari.server.Role;

/**
 * Represents minimized version of {@link org.apache.ambari.server.actionmanager.HostRoleCommand}.
 * Provides only info which is required to calculate {@link HostRoleStatus}
 * for {@link org.apache.ambari.server.actionmanager.Stage}.
 */
public class HostRoleCommandMinimal {

  private long taskId;
  private HostRoleStatus status;
  private Role role;

  public HostRoleCommandMinimal(HostRoleCommand existing) {
    this.taskId = existing.getTaskId();
    this.status = existing.getStatus();
    this.role = existing.getRole();
  }

  public HostRoleCommandMinimal(Long taskId, HostRoleStatus status, Role role) {
    this.taskId = taskId;
    this.status = status;
    this.role = role;
  }

  public long getTaskId() {
    return taskId;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public Role getRole() {
    return role;
  }
}
