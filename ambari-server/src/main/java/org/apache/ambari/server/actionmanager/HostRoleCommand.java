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

import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.ServiceComponentHostEvent;

/**
 * This class encapsulates the information for an task on a host for a
 * particular role which action manager needs. It doesn't capture actual
 * command and parameters, but just the stuff enough for action manager to
 * track the request.
 * For the actual command refer {@link HostAction#commandToHost}
 */
public class HostRoleCommand {
  private final Role role;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String stdout = "";
  private String stderr = "";
  private int exitCode = 999; //Default is unknown
  private final ServiceComponentHostEvent event;

  public HostRoleCommand(Role role, ServiceComponentHostEvent event) {
    this.role = role;
    this.event = event;
  }

  public Role getRole() {
    return role;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public ServiceComponentHostEvent getEvent() {
    return event;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }
}
