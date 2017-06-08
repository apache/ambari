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

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.topology.TopologyManager;

public class RequestUpdateEvent extends AmbariUpdateEvent {

  private Long clusterId;
  private Long endTime;
  private Long requestId;
  private Double progressPercent;
  private String requestContext;
  private HostRoleStatus requestStatus;
  private Long startTime;


  public RequestUpdateEvent(RequestEntity requestEntity, HostRoleCommandDAO hostRoleCommandDAO, TopologyManager topologyManager) {
    super(Type.REQUEST);
    this.clusterId = requestEntity.getClusterId();
    this.endTime = requestEntity.getEndTime();
    this.requestId = requestEntity.getRequestId();
    this.progressPercent = CalculatedStatus.statusFromRequest(hostRoleCommandDAO, topologyManager, requestEntity.getRequestId()).getPercent();
    this.requestContext = requestEntity.getRequestContext();
    this.requestStatus = requestEntity.getStatus();
    this.startTime = requestEntity.getStartTime();
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
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
}
