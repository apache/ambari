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

package org.apache.ambari.server.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.topology.TopologyManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestUpdateEvent extends AmbariUpdateEvent {

  private String clusterName;
  private Long endTime;
  private Long requestId;
  private Double progressPercent;
  private String requestContext;
  private HostRoleStatus requestStatus;
  private Long startTime;

  @JsonProperty("Tasks")
  private List<HostRoleCommand> hostRoleCommands = new ArrayList<>();

  public RequestUpdateEvent(RequestEntity requestEntity,
                            HostRoleCommandDAO hostRoleCommandDAO,
                            TopologyManager topologyManager,
                            String clusterName,
                            List<HostRoleCommandEntity> hostRoleCommandEntities) {
    super(Type.REQUEST);
    this.clusterName = clusterName;
    this.endTime = requestEntity.getEndTime();
    this.requestId = requestEntity.getRequestId();
    this.progressPercent = CalculatedStatus.statusFromRequest(hostRoleCommandDAO, topologyManager, requestEntity.getRequestId()).getPercent();
    this.requestContext = requestEntity.getRequestContext();
    this.requestStatus = requestEntity.getStatus();
    this.startTime = requestEntity.getStartTime();

    for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandEntities) {
      hostRoleCommands.add(new HostRoleCommand(hostRoleCommandEntity.getTaskId(),
          hostRoleCommandEntity.getRequestId(),
          hostRoleCommandEntity.getStatus(),
          hostRoleCommandEntity.getHostName()));
    }
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Double getProgressPercent() {
    return progressPercent;
  }

  public void setProgressPercent(Double progressPercent) {
    this.progressPercent = progressPercent;
  }

  public HostRoleStatus getRequestStatus() {
    return requestStatus;
  }

  public void setRequestStatus(HostRoleStatus requestStatus) {
    this.requestStatus = requestStatus;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public class HostRoleCommand {
    private Long id;
    private Long requestId;
    private HostRoleStatus status;
    private String hostName;

    public HostRoleCommand(Long id, Long requestId, HostRoleStatus status, String hostName) {
      this.id = id;
      this.requestId = requestId;
      this.status = status;
      this.hostName = hostName;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getRequestId() {
      return requestId;
    }

    public void setRequestId(Long requestId) {
      this.requestId = requestId;
    }

    public HostRoleStatus getStatus() {
      return status;
    }

    public void setStatus(HostRoleStatus status) {
      this.status = status;
    }

    public String getHostName() {
      return hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }
  }
}
