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

package org.apache.ambari.server.controller;


import java.util.List;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.inject.Inject;

@StaticallyInject
public class ServiceConfigVersionResponse {
  /**
   * Name used for default config group.
   */
  public static final String DEFAULT_CONFIG_GROUP_NAME = "Default";

  /**
   * Name used for config groups that were deleted in the service config version response.
   */
  public static final String DELETED_CONFIG_GROUP_NAME = "Deleted";


  @JsonProperty("cluster_name")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  private final String clusterName;

  @JsonProperty("service_name")
  private final String serviceName;

  @JsonProperty("service_config_version")
  private final Long version;

  @JsonProperty("createtime")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  private final Long createTime;

  @JsonProperty("group_id")
  private final Long groupId;

  @JsonProperty("group_name")
  private final String groupName;

  @JsonProperty("user")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  private final String userName;

  @JsonProperty("service_config_version_note")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  private final String note;

  @JsonProperty("stack_id")
  private String stackId;

  @JsonProperty("is_current")
  private Boolean isCurrent = Boolean.FALSE;

  @JsonProperty("is_cluster_compatible")
  private final Boolean isCompatibleWithCurrentStack;

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  private List<ConfigurationResponse> configurations;

  @JsonProperty("hosts")
  private final List<String> hosts;

  @Inject
  private static HostDAO hostDAO;

  /**
   * Constructor.
   *
   * @param serviceConfigEntity
   * @param configGroupName
   */
  public ServiceConfigVersionResponse(ServiceConfigEntity serviceConfigEntity,
      String configGroupName) {
    super();
    ClusterEntity clusterEntity = serviceConfigEntity.getClusterEntity();

    clusterName = clusterEntity.getClusterName();
    serviceName = serviceConfigEntity.getServiceName();
    version = serviceConfigEntity.getVersion();
    userName = serviceConfigEntity.getUser();
    createTime = serviceConfigEntity.getCreateTimestamp();
    note = serviceConfigEntity.getNote();
    groupId = (null != serviceConfigEntity.getGroupId() ? serviceConfigEntity.getGroupId(): -1L);
    groupName = configGroupName;
    hosts = hostDAO.getHostNamesByHostIds(serviceConfigEntity.getHostIds());

    StackEntity serviceConfigStackEntity = serviceConfigEntity.getStack();
    StackEntity clusterStackEntity = clusterEntity.getClusterStateEntity().getCurrentStack();

    isCompatibleWithCurrentStack = clusterStackEntity.equals(serviceConfigStackEntity);
    stackId = new StackId(serviceConfigStackEntity).getStackId();
  }

  public String getServiceName() {
    return serviceName;
  }

  public Long getVersion() {
    return version;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public String getUserName() {
    return userName;
  }
  public String getClusterName() {
    return clusterName;
  }

  public List<ConfigurationResponse> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(List<ConfigurationResponse> configurations) {
    this.configurations = configurations;
  }

  public String getNote() {
    return note;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public String getGroupName() {
    return groupName;
  }

  public Long getGroupId() {
    return groupId;
  }

  /**
   * Gets the Stack ID that this configuration is scoped for.
   *
   * @return
   */
  public String getStackId() {
    return stackId;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  /**
   * Gets whether this service configuration is compatible with the cluster's
   * current stack version.
   *
   * @return {@code true} if compatible, {@code false} otherwise.
   */
  public Boolean isCompatibleWithCurrentStack() {
    return isCompatibleWithCurrentStack;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ServiceConfigVersionResponse)) return false;

    ServiceConfigVersionResponse that = (ServiceConfigVersionResponse) o;

    return new EqualsBuilder()
      .append(clusterName, that.clusterName)
      .append(serviceName, that.serviceName)
      .append(version, that.version)
      .append(createTime, that.createTime)
      .append(groupId, that.groupId)
      .append(groupName, that.groupName)
      .append(userName, that.userName)
      .append(note, that.note)
      .append(stackId, that.stackId)
      .append(isCurrent, that.isCurrent)
      .append(isCompatibleWithCurrentStack, that.isCompatibleWithCurrentStack)
      .append(configurations, that.configurations)
      .append(hosts, that.hosts)
      .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(clusterName)
      .append(serviceName)
      .append(version)
      .append(createTime)
      .append(groupId)
      .append(groupName)
      .append(userName)
      .append(note)
      .append(stackId)
      .append(isCurrent)
      .append(isCompatibleWithCurrentStack)
      .append(configurations)
      .append(hosts)
      .toHashCode();
  }
}

