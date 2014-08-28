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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.StackVersionResponse;

public class StackInfo implements Comparable<StackInfo>{
  private String name;
  private String version;
  private String minUpgradeVersion;
  private boolean active;
  private String rcoFileLocation;
  private List<RepositoryInfo> repositories;
  private List<ServiceInfo> services;
  private String parentStackVersion;
  // stack-level properties
  private List<PropertyInfo> properties;
  private Map<String, Map<String, Map<String, String>>> configTypes;

  /**
   * Meaning: stores subpath from stack root to exact hooks folder for stack. These hooks are
   * applied to all commands for services in current stack.
   */
  private String stackHooksFolder;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<RepositoryInfo> getRepositories() {
    if( repositories == null ) repositories = new ArrayList<RepositoryInfo>();
    return repositories;
  }

  public void setRepositories(List<RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public synchronized List<ServiceInfo> getServices() {
    if (services == null) services = new ArrayList<ServiceInfo>();
    return services;
  }

  public synchronized void setServices(List<ServiceInfo> services) {
    this.services = services;
  }
  
  public List<PropertyInfo> getProperties() {
    if (properties == null) properties = new ArrayList<PropertyInfo>();
    return properties;
  }

  public void setProperties(List<PropertyInfo> properties) {
    this.properties = properties;
  }
  
  public Map<String, Map<String, Map<String, String>>> getConfigTypes() {
    if (configTypes == null) configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    return configTypes;
  }

  public void setConfigTypes(
      Map<String, Map<String, Map<String, String>>> configTypes) {
    this.configTypes = configTypes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Stack name:" + name + "\nversion:" +
      version + "\nactive:" + active);
    if (services != null) {
      sb.append("\n\t\tService:");
      for (ServiceInfo service : services) {
        sb.append("\t\t" + service.toString());
      }
    }

    if (repositories != null) {
      sb.append("\n\t\tRepositories:");
      for (RepositoryInfo repository : repositories) {
        sb.append("\t\t" + repository.toString());
      }
    }

    return sb.toString();
  }
  
  
  @Override
  public int hashCode() {
    int result = 1;
    result = 31  + name.hashCode() + version.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof StackInfo)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    StackInfo stackInfo = (StackInfo) obj;
    return getName().equals(stackInfo.getName()) && getVersion().equals(stackInfo.getVersion());
  }

  public StackVersionResponse convertToResponse() {

    return new StackVersionResponse(getVersion(), getMinUpgradeVersion(),
      isActive(), getParentStackVersion(), getConfigTypes());
  }

  public String getMinUpgradeVersion() {
    return minUpgradeVersion;
  }

  public void setMinUpgradeVersion(String minUpgradeVersion) {
    this.minUpgradeVersion = minUpgradeVersion;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getParentStackVersion() {
    return parentStackVersion;
  }

  public void setParentStackVersion(String parentStackVersion) {
    this.parentStackVersion = parentStackVersion;
  }

  public String getRcoFileLocation() {
    return rcoFileLocation;
  }

  public void setRcoFileLocation(String rcoFileLocation) {
    this.rcoFileLocation = rcoFileLocation;
  }

  public String getStackHooksFolder() {
    return stackHooksFolder;
  }

  public void setStackHooksFolder(String stackHooksFolder) {
    this.stackHooksFolder = stackHooksFolder;
  }

  @Override
  public int compareTo(StackInfo o) {
    String myId = name + "-" + version;
    String oId = o.name + "-" + o.version;
    return myId.compareTo(oId);
  }
}
