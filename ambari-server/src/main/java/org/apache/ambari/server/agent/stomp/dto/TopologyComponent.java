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

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyComponent {
  private String componentName;
  private String serviceName;
  private String version;
  private Set<Long> hostIds;
  private TopologyStatusCommandParams statusCommandParams;

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

    public Builder setVersion(String version) {
      TopologyComponent.this.setVersion(version);
      return this;
    }

    public Builder setHostIds(Set<Long> hostIds) {
      TopologyComponent.this.setHostIds(hostIds);
      return this;
    }

    public Builder setStatusCommandParams(TopologyStatusCommandParams statusCommandParams) {
      TopologyComponent.this.setStatusCommandParams(statusCommandParams);
      return this;
    }

    public TopologyComponent build() {
      return TopologyComponent.this;
    }
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

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Set<Long> getHostIds() {
    return hostIds;
  }

  public void setHostIds(Set<Long> hostIds) {
    this.hostIds = hostIds;
  }

  public void addHostId(Long hostId) {
    this.hostIds.add(hostId);
  }

  public TopologyStatusCommandParams getStatusCommandParams() {
    return statusCommandParams;
  }

  public void setStatusCommandParams(TopologyStatusCommandParams statusCommandParams) {
    this.statusCommandParams = statusCommandParams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyComponent that = (TopologyComponent) o;

    if (!componentName.equals(that.componentName)) return false;
    return version.equals(that.version);
  }

  @Override
  public int hashCode() {
    int result = componentName.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }
}
