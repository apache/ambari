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

package org.apache.ambari.server.agent.stomp.dto;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.state.State;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TopologyComponent {
  private String componentName;
  private String serviceName;
  private String serviceGroupName;
  private String serviceType;
  private String version;
  private String mpackVersion;
  private Set<Long> hostIds = new HashSet<>();
  private Set<String> hostNames = new HashSet<>();
  private Set<String> publicHostNames = new HashSet<>();
  private TreeMap<String, String> componentLevelParams = new TreeMap<>();
  private TreeMap<String, String> commandParams = new TreeMap<>();
  private State lastComponentState;

  private TopologyComponent() {
  }

  public static Builder newBuilder() {
    return new TopologyComponent().new Builder();
  }

  public class Builder {
    private Builder() {

    }

    public Builder setComponentName(String componentName) {
      TopologyComponent.this.setComponentName(componentName);
      return this;
    }

    public Builder setServiceName(String serviceName) {
      TopologyComponent.this.setServiceName(serviceName);
      return this;
    }

    public Builder setServiceGroupName(String serviceGroupName) {
      TopologyComponent.this.setServiceGroupName(serviceGroupName);
      return this;
    }

    public Builder setServiceType(String serviceType) {
      TopologyComponent.this.setServiceType(serviceType);
      return this;
    }

    public Builder setVersion(String version) {
      TopologyComponent.this.setVersion(version);
      return this;
    }

    public Builder setMpackVersion(String mpackVersion) {
      TopologyComponent.this.setMpackVersion(mpackVersion);
      return this;
    }

    public Builder setHostIds(Set<Long> hostIds) {
      TopologyComponent.this.setHostIds(hostIds);
      return this;
    }

    public Builder setHostNames(Set<String> hostNames) {
      TopologyComponent.this.setHostNames(hostNames);
      return this;
    }

    public Builder setPublicHostNames(Set<String> publicHostNames) {
      TopologyComponent.this.setPublicHostNames(publicHostNames);
      return this;
    }

    public Builder setComponentLevelParams(TreeMap<String, String> componentLevelParams) {
      TopologyComponent.this.setComponentLevelParams(componentLevelParams);
      return this;
    }

    public Builder setCommandParams(TreeMap<String, String> commandParams) {
      TopologyComponent.this.setCommandParams(commandParams);
      return this;
    }

    public Builder setLastComponentState(State lastComponentState) {
      TopologyComponent.this.setLastComponentState(lastComponentState);
      return this;
    }

    public TopologyComponent build() {
      return TopologyComponent.this;
    }
  }

  public boolean updateComponent(TopologyComponent componentToUpdate) {
    boolean changed = false;
    //TODO will be a need to change to multi-instance usage
    if (componentToUpdate.getComponentName().equals(getComponentName())) {
      if (StringUtils.isNotEmpty(componentToUpdate.getVersion()) && !componentToUpdate.getVersion().equals(getVersion())) {
        setVersion(componentToUpdate.getVersion());
        changed = true;
      }
      if (CollectionUtils.isNotEmpty(componentToUpdate.getHostIds())) {
        if (hostIds == null) {
          hostIds = new HashSet<>();
        }
        changed |= hostIds.addAll(componentToUpdate.getHostIds());
      }
      if (CollectionUtils.isNotEmpty(componentToUpdate.getHostNames())) {
        if (hostNames == null) {
          hostNames = new HashSet<>();
        }
        changed |= hostNames.addAll(componentToUpdate.getHostNames());
      }
      if (CollectionUtils.isNotEmpty(componentToUpdate.getPublicHostNames())) {
        if (publicHostNames == null) {
          publicHostNames = new HashSet<>();
        }
        changed |= publicHostNames.addAll(componentToUpdate.getPublicHostNames());
      }
      changed |= mergeParams(componentLevelParams, componentToUpdate.getComponentLevelParams());
      changed |= mergeParams(commandParams, componentToUpdate.getCommandParams());
    }
    return changed;
  }

  private boolean mergeParams(TreeMap<String, String> currentParams, TreeMap<String, String> updateParams) {
    boolean changed = false;
    if (MapUtils.isNotEmpty(updateParams)) {
      for (Map.Entry<String, String> updateParam : updateParams.entrySet()) {
        String updateParamName = updateParam.getKey();
        String updateParamValue = updateParam.getValue();
        if (!currentParams.containsKey(updateParamName) ||
            !StringUtils.equals(currentParams.get(updateParamName), updateParamValue)) {
          currentParams.put(updateParamName, updateParamValue);
          changed = true;
        }
      }
    }
    return changed;
  }

  public boolean removeComponent(TopologyComponent componentToRemove) {
    boolean changed = false;
    if (componentToRemove.getComponentName().equals(getComponentName())) {
      if (CollectionUtils.isNotEmpty(componentToRemove.getHostIds())) {
        if (hostIds != null) {
          hostIds.removeAll(componentToRemove.getHostIds());
          changed = true;
        }
      }
      if (CollectionUtils.isNotEmpty(componentToRemove.getHostNames())) {
        if (hostNames != null) {
          hostNames.removeAll(componentToRemove.getHostNames());
          changed = true;
        }
      }
      if (CollectionUtils.isNotEmpty(componentToRemove.getPublicHostNames())) {
        if (publicHostNames != null) {
          publicHostNames.removeAll(componentToRemove.getPublicHostNames());
          changed = true;
        }
      }
    }
    return changed;
  }

  public  TopologyComponent deepCopy() {
    return TopologyComponent.newBuilder().setComponentName(getComponentName())
        .setServiceType(getServiceType())
        .setServiceName(getServiceName())
        .setServiceGroupName(getServiceGroupName())
        .setComponentLevelParams(getComponentLevelParams() == null ? null : new TreeMap<>(getComponentLevelParams()))
        .setHostIds(getHostIds() == null ? null : new HashSet<>(getHostIds()))
        .setHostNames(getHostNames() == null ? null : new HashSet<>(getHostNames()))
        .setPublicHostNames(getPublicHostNames() == null ? null : new HashSet<>(getPublicHostNames()))
        .setCommandParams(getCommandParams() == null ? null : new TreeMap<>(getCommandParams()))
        .build();
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public Set<Long> getHostIds() {
    return hostIds;
  }

  public void setHostIds(Set<Long> hostIds) {
    this.hostIds = hostIds;
  }

  public void addHostId(Long hostId) {
    hostIds.add(hostId);
  }

  public void addHostName(String hostName) {
    hostNames.add(hostName);
  }

  public TreeMap<String, String> getComponentLevelParams() {
    return componentLevelParams;
  }

  public void setComponentLevelParams(TreeMap<String, String> componentLevelParams) {
    this.componentLevelParams = componentLevelParams;
  }

  public Set<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(Set<String> hostNames) {
    this.hostNames = hostNames;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public Set<String> getPublicHostNames() {
    return publicHostNames;
  }

  public void setPublicHostNames(Set<String> publicHostNames) {
    this.publicHostNames = publicHostNames;
  }

  public TreeMap<String, String> getCommandParams() {
    return commandParams;
  }

  public void setCommandParams(TreeMap<String, String> commandParams) {
    this.commandParams = commandParams;
  }

  public State getLastComponentState() {
    return lastComponentState;
  }

  public void setLastComponentState(State lastComponentState) {
    this.lastComponentState = lastComponentState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TopologyComponent that = (TopologyComponent) o;
    return Objects.equals(serviceGroupName, that.serviceGroupName)
        && Objects.equals(serviceName, that.serviceName)
        && Objects.equals(componentName, that.componentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceGroupName, serviceName, componentName);
  }
}
