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

import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;

public class ConfigsUpdateEvent extends AmbariUpdateEvent {

  private Long serviceConfigId;
  private Long clusterId;
  private String serviceName;
  private Long groupId;
  private Long version;
  private String user;
  private String note;
  private List<String> hostNames;
  private Long createtime;
  private String groupName;
  //TODO configs

  private List<ClusterConfig> configs = new ArrayList<>();

  public ConfigsUpdateEvent(ServiceConfigEntity configs, String configGroupName, List<String> hostNames) {
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
    this.createtime = configs.getCreateTimestamp();
    this.groupName = configGroupName;
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

  public Long getCreatetime() {
    return createtime;
  }

  public void setCreatetime(Long createtime) {
    this.createtime = createtime;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
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
  }
}
