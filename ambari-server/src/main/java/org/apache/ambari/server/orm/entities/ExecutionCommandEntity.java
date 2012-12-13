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

package org.apache.ambari.server.orm.entities;

import javax.persistence.*;

@Table(name = "execution_command", schema = "ambari", catalog = "")
@Entity
public class ExecutionCommandEntity {
  private Long taskId;

  @Column(name = "task_id", insertable = false, updatable = false, nullable = false)
  @Id
  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  private byte[] command;

  @Column(name = "command")
  @Lob
  @Basic
  public byte[] getCommand() {
    return command;
  }

  public void setCommand(byte[] command) {
    this.command = command;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutionCommandEntity that = (ExecutionCommandEntity) o;

    if (command != null ? !command.equals(that.command) : that.command != null) return false;
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (command != null ? command.hashCode() : 0);
    return result;
  }

  private HostRoleCommandEntity hostRoleCommand;

  @OneToOne
  @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false)
  public HostRoleCommandEntity getHostRoleCommand() {
    return hostRoleCommand;
  }

  public void setHostRoleCommand(HostRoleCommandEntity hostRoleCommandByTaskId) {
    this.hostRoleCommand = hostRoleCommandByTaskId;
  }
}
