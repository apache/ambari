/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contains info about configs update. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigsUpdateEvent extends AmbariUpdateEvent {

  private Long serviceConfigId;
  private Long clusterId;
  private String serviceName;
  private Long groupId;
  private Long version;
  private String user;
  private String note;
  private List<String> hostNames;
  private Long createTime;
  private String groupName;

  private List<ClusterConfig> configs = new ArrayList<>();
  private Set<String> changedConfigTypes = new HashSet<>();

  public ConfigsUpdateEvent(ServiceConfigEntity configs, String configGroupName, List<String> hostNames,
                            Set<String> changedConfigTypes) {
    super(Type.CONFIGS);
    this.serviceConfigId = configs.getServiceConfigId();
    this.clusterId = configs.getClusterEntity().getClusterId();
    this.serviceName = configs.getServiceName();
    this.groupId = configs.getGroupId();
    this.version = configs.getVersion();
    this.user = configs.getUser();
    this.note = configs.getNote();
    this.hostNames = hostNames == null ? null : new ArrayList<>(hostNames);
    for (ClusterConfigEntity clusterConfigEntity : configs.getClusterConfigEntities()) {
      this.configs.add(new ClusterConfig(clusterConfigEntity.getClusterId(),
        clusterConfigEntity.getType(),
        clusterConfigEntity.getTag(),
        clusterConfigEntity.getVersion()));
    }
    this.createTime = configs.getCreateTimestamp();
    this.groupName = configGroupName;
    this.changedConfigTypes = changedConfigTypes;
  }

  public ConfigsUpdateEvent(Cluster cluster, Collection<ClusterConfigEntity> configs) {
    super(Type.CONFIGS);
    this.clusterId = cluster.getClusterId();
    for (ClusterConfigEntity clusterConfigEntity : configs) {
      this.configs.add(new ClusterConfig(clusterConfigEntity.getClusterId(),
          clusterConfigEntity.getType(),
          clusterConfigEntity.getTag(),
          clusterConfigEntity.getVersion()));
    }
  }

  public Long getServiceConfigId() {
    return serviceConfigId;
  }

  public void setServiceConfigId(Long serviceConfigId) {
    this.serviceConfigId = serviceConfigId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  public List<ClusterConfig> getConfigs() {
    return configs;
  }

  public void setConfigs(List<ClusterConfig> configs) {
    this.configs = configs;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public Set<String> getChangedConfigTypes() {
    return changedConfigTypes;
  }

  public void setChangedConfigTypes(Set<String> changedConfigTypes) {
    this.changedConfigTypes = changedConfigTypes;
  }

  public class ClusterConfig {
    private Long clusterId;
    private String type;
    private String tag;
    private Long version;

    public ClusterConfig(Long clusterId, String type, String tag, Long version) {
      this.clusterId = clusterId;
      this.type = type;
      this.tag = tag;
      this.version = version;
    }

    public Long getClusterId() {
      return clusterId;
    }

    public void setClusterId(Long clusterId) {
      this.clusterId = clusterId;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public Long getVersion() {
      return version;
    }

    public void setVersion(Long version) {
      this.version = version;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ClusterConfig that = (ClusterConfig) o;

      if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
      if (type != null ? !type.equals(that.type) : that.type != null) return false;
      if (tag != null ? !tag.equals(that.tag) : that.tag != null) return false;
      return version != null ? version.equals(that.version) : that.version == null;
    }

    @Override
    public int hashCode() {
      int result = clusterId != null ? clusterId.hashCode() : 0;
      result = 31 * result + (type != null ? type.hashCode() : 0);
      result = 31 * result + (tag != null ? tag.hashCode() : 0);
      result = 31 * result + (version != null ? version.hashCode() : 0);
      return result;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigsUpdateEvent that = (ConfigsUpdateEvent) o;

    if (serviceConfigId != null ? !serviceConfigId.equals(that.serviceConfigId) : that.serviceConfigId != null)
      return false;
    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) return false;
    if (version != null ? !version.equals(that.version) : that.version != null) return false;
    if (user != null ? !user.equals(that.user) : that.user != null) return false;
    if (note != null ? !note.equals(that.note) : that.note != null) return false;
    if (hostNames != null ? !hostNames.equals(that.hostNames) : that.hostNames != null) return false;
    if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null) return false;
    if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) return false;
    if (configs != null ? !configs.equals(that.configs) : that.configs != null) return false;
    return changedConfigTypes != null ? changedConfigTypes.equals(that.changedConfigTypes) : that.changedConfigTypes == null;
  }

  @Override
  public int hashCode() {
    int result = serviceConfigId != null ? serviceConfigId.hashCode() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (user != null ? user.hashCode() : 0);
    result = 31 * result + (note != null ? note.hashCode() : 0);
    result = 31 * result + (hostNames != null ? hostNames.hashCode() : 0);
    result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
    result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
    result = 31 * result + (configs != null ? configs.hashCode() : 0);
    result = 31 * result + (changedConfigTypes != null ? changedConfigTypes.hashCode() : 0);
    return result;
  }
}
