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
package org.apache.ambari.server.controller.internal;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.ambari.server.AmbariException;

@NotThreadSafe
public class DeleteHostComponentStatusMetaData extends DeleteStatusMetaData {
  private Set<HostComponent> removedHostComponents;
  private AmbariException ambariException;
  public DeleteHostComponentStatusMetaData() {
    removedHostComponents = new HashSet<>();
  }

  public void addDeletedHostComponent(String componentName, String hostName, Long hostId,
                                      String clusterId, String version) {
    removedHostComponents.add(new HostComponent(componentName, hostId, clusterId, version));
    addDeletedKey(componentName + "/" + hostName);
  }

  public Set<HostComponent> getRemovedHostComponents() {
    return removedHostComponents;
  }

  public AmbariException getAmbariException() {
    return ambariException;
  }

  public void setAmbariException(AmbariException ambariException) {
    this.ambariException = ambariException;
  }

  public class HostComponent {
    private String componentName;
    private Long hostId;
    private String clusterId;
    private String version;

    public HostComponent(String componentName, Long hostId, String clusterId, String version) {
      this.componentName = componentName;
      this.hostId = hostId;
      this.clusterId = clusterId;
      this.version = version;
    }

    public String getComponentName() {
      return componentName;
    }

    public void setComponentName(String componentName) {
      this.componentName = componentName;
    }

    public Long getHostId() {
      return hostId;
    }

    public void setHostName(Long hostId) {
      this.hostId = hostId;
    }

    public String getClusterId() {
      return clusterId;
    }

    public void setClusterId(String clusterId) {
      this.clusterId = clusterId;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }
}
