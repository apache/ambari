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

import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestResourceFilterEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Request {
  private static final Logger LOG = LoggerFactory.getLogger(Request.class);

  private final long requestId;
  private final long clusterId;
  private final String clusterName;
  private Long requestScheduleId;
  private String commandName;
  private String requestContext;
  private long createTime;
  private long startTime;
  private long endTime;
  private HostRoleStatus status; // not persisted yet
  private String inputs;
  private List<RequestResourceFilter> resourceFilters;
  private RequestType requestType;

  private Collection<Stage> stages = new ArrayList<Stage>();

  @AssistedInject
  /**
   * Construct new entity
   */
  public Request(@Assisted long requestId, @Assisted("clusterId") Long clusterId, Clusters clusters) {
    this.requestId = requestId;
    this.clusterId = clusterId;
    this.createTime = System.currentTimeMillis();
    this.startTime = -1;
    this.endTime = -1;
    try {
      this.clusterName = clusters.getClusterById(clusterId).getClusterName();
    } catch (AmbariException e) {
      String message = String.format("Cluster with id=%s not found", clusterId);
      LOG.error(message);
      throw new RuntimeException(message);
    }
  }

  @AssistedInject
  /**
   * Construct new entity from stages provided
   */
  //TODO remove when not needed
  public Request(@Assisted Collection<Stage> stages, Clusters clusters){
    if (stages != null && !stages.isEmpty()) {
      this.stages.addAll(stages);
      Stage stage = stages.iterator().next();
      this.requestId = stage.getRequestId();
      this.clusterName = stage.getClusterName();
      try {
        this.clusterId = clusters.getCluster(clusterName).getClusterId();
      } catch (AmbariException e) {
        String message = String.format("Cluster %s not found", clusterName);
        LOG.error(message);
        throw new RuntimeException(message);
      }
      this.requestContext = stages.iterator().next().getRequestContext();
      this.createTime = System.currentTimeMillis();
      this.startTime = -1;
      this.endTime = -1;
      this.requestType = RequestType.INTERNAL_REQUEST;
    } else {
      String message = "Attempted to construct request from empty stage collection";
      LOG.error(message);
      throw new RuntimeException(message);
    }
  }

  @AssistedInject
  /**
   * Construct new entity from stages provided
   */
  //TODO remove when not needed
  public Request(@Assisted Collection<Stage> stages, @Assisted ExecuteActionRequest actionRequest,
                 Clusters clusters, Gson gson) throws AmbariException {
    this(stages, clusters);
    if (actionRequest != null) {
      this.resourceFilters = actionRequest.getResourceFilters();
      this.inputs = gson.toJson(actionRequest.getParameters());
      this.requestType = actionRequest.isCommand() ? RequestType.COMMAND : RequestType.ACTION;
      this.commandName = actionRequest.isCommand() ? actionRequest.getCommandName() : actionRequest.getActionName();
    }
  }

  @AssistedInject
  /**
   * Load existing request from database
   */
  public Request(@Assisted RequestEntity entity, StageFactory stageFactory){
    if (entity == null) {
      throw new RuntimeException("Request entity cannot be null.");
    }

    this.requestId = entity.getRequestId();
    this.clusterId = entity.getCluster().getClusterId();
    this.clusterName = entity.getCluster().getClusterName();
    this.createTime = entity.getCreateTime();
    this.startTime = entity.getStartTime();
    this.endTime = entity.getEndTime();
    this.requestContext = entity.getRequestContext();
    this.inputs = entity.getInputs();

    this.requestType = entity.getRequestType();
    this.commandName = entity.getCommandName();
    this.status = entity.getStatus();
    if (entity.getRequestScheduleEntity() != null) {
      this.requestScheduleId = entity.getRequestScheduleEntity().getScheduleId();
    }

    for (StageEntity stageEntity : entity.getStages()) {
      Stage stage = stageFactory.createExisting(stageEntity);
      stages.add(stage);
    }

    for (RequestResourceFilterEntity resourceFilterEntity : entity.getResourceFilterEntities()) {
      RequestResourceFilter resourceFilter =
        new RequestResourceFilter(
            resourceFilterEntity.getServiceName(),
            resourceFilterEntity.getComponentName(),
            getHostsList(resourceFilterEntity.getHosts()));
      this.resourceFilters.add(resourceFilter);
    }

  }

  private List<String> getHostsList(String hosts) {
    List<String> hostList = new ArrayList<String>();
    if (hosts != null && !hosts.isEmpty()) {
      for (String host : hosts.split(",")) {
        if (!host.trim().isEmpty()) {
          hostList.add(host.trim());
        }
      }
    }
    return hostList;
  }

  public Collection<Stage> getStages() {
    return stages;
  }

  public void setStages(Collection<Stage> stages) {
    this.stages = stages;
  }

  public long getRequestId() {
    return requestId;
  }

  public synchronized RequestEntity constructNewPersistenceEntity() {
    RequestEntity requestEntity = new RequestEntity();

    requestEntity.setRequestId(requestId);
    requestEntity.setClusterId(clusterId);
    requestEntity.setCreateTime(createTime);
    requestEntity.setStartTime(startTime);
    requestEntity.setEndTime(endTime);
    requestEntity.setRequestContext(requestContext);
    requestEntity.setInputs(inputs);
    requestEntity.setRequestType(requestType);
    requestEntity.setRequestScheduleId(requestScheduleId);
    //TODO set all fields

    if (resourceFilters != null) {
      List<RequestResourceFilterEntity> filterEntities = new ArrayList<RequestResourceFilterEntity>();
      for (RequestResourceFilter resourceFilter : resourceFilters) {
        RequestResourceFilterEntity filterEntity = new RequestResourceFilterEntity();
        filterEntity.setServiceName(resourceFilter.getServiceName());
        filterEntity.setComponentName(resourceFilter.getComponentName());
        filterEntity.setRequestEntity(requestEntity);
        filterEntity.setRequestId(requestId);
      }
      requestEntity.setResourceFilterEntities(filterEntities);
    }

    return requestEntity;
  }


  public Long getClusterId() {
    return clusterId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public List<RequestResourceFilter> getResourceFilters() {
    return resourceFilters;
  }

  public void setResourceFilters(List<RequestResourceFilter> resourceFilters) {
    this.resourceFilters = resourceFilters;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public Long getRequestScheduleId() {
    return requestScheduleId;
  }

  public void setRequestScheduleId(Long requestScheduleId) {
    this.requestScheduleId = requestScheduleId;
  }

  public List<HostRoleCommand> getCommands() {
    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();
    for (Stage stage : stages) {
      commands.addAll(stage.getOrderedHostRoleCommands());
    }
    return commands;
  }

  @Override
  public String toString() {
    return "Request{" +
        "requestId=" + requestId +
        ", clusterId=" + clusterId +
        ", clusterName='" + clusterName + '\'' +
        ", requestContext='" + requestContext + '\'' +
        ", createTime=" + createTime +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", inputs='" + inputs + '\'' +
        ", resourceFilters='" + resourceFilters + '\'' +
        ", requestType=" + requestType +
        ", stages=" + stages +
        '}';
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }
}
