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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.StackVersionResponse;
import org.apache.ambari.server.state.stack.UpgradePack;

public class StackInfo implements Comparable<StackInfo>{
  private String name;
  private String version;
  private String minUpgradeVersion;
  private boolean active;
  private String rcoFileLocation;
  private List<RepositoryInfo> repositories;
  private Collection<ServiceInfo> services;
  private String parentStackVersion;
  // stack-level properties
  private List<PropertyInfo> properties;
  private Map<String, Map<String, Map<String, String>>> configTypes;
  private Map<String, UpgradePack> upgradePacks;

  /**
   * Meaning: stores subpath from stack root to exact hooks folder for stack. These hooks are
   * applied to all commands for services in current stack.
   */
  private String stackHooksFolder;

  private String upgradesFolder = null;

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

  public synchronized Collection<ServiceInfo> getServices() {
    if (services == null) services = new ArrayList<ServiceInfo>();
    return services;
  }

  public ServiceInfo getService(String name) {
    Collection<ServiceInfo> services = getServices();
    for (ServiceInfo service : services) {
      if (service.getName().equals(name)) {
        return service;
      }
    }
    //todo: exception?
    return null;
  }

  public synchronized void setServices(Collection<ServiceInfo> services) {
    this.services = services;
  }

  public List<PropertyInfo> getProperties() {
    if (properties == null) properties = new ArrayList<PropertyInfo>();
    return properties;
  }

  public void setProperties(List<PropertyInfo> properties) {
    this.properties = properties;
  }

  /**
   * Obtain the config types associated with this stack.
   * The returned map is an unmodifiable view.
   * @return copy of the map of config types associated with this stack
   */
  public synchronized Map<String, Map<String, Map<String, String>>> getConfigTypeAttributes() {
    return configTypes == null ?
        Collections.<String, Map<String, Map<String, String>>>emptyMap() :
        Collections.unmodifiableMap(configTypes);
  }


  /**
   * Add the given type and set it's attributes.
   *
   * @param type            configuration type
   * @param typeAttributes  attributes associated with the type
   */
  public synchronized void setConfigTypeAttributes(String type, Map<String, Map<String, String>> typeAttributes) {
    if (this.configTypes == null) {
      configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    }
    // todo: no exclusion mechanism for stack config types
    configTypes.put(type, typeAttributes);
  }

  /**
   * Set all types and associated attributes.  Any previously existing types and
   * attributes are removed prior to setting the new values.
   *
   * @param types map of type attributes
   */
  public synchronized void setAllConfigAttributes(Map<String, Map<String, Map<String, String>>> types) {
    configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    for (Map.Entry<String, Map<String, Map<String, String>>> entry : types.entrySet()) {
      setConfigTypeAttributes(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Stack name:" + name + "\nversion:" +
      version + "\nactive:" + active);
    if (services != null) {
      sb.append("\n\t\tService:");
      for (ServiceInfo service : services) {
        sb.append("\t\t");
        sb.append(service);
      }
    }

    if (repositories != null) {
      sb.append("\n\t\tRepositories:");
      for (RepositoryInfo repository : repositories) {
        sb.append("\t\t");
        sb.append(repository.toString());
      }
    }

    return sb.toString();
  }


  @Override
  public int hashCode() {
    return 31  + name.hashCode() + version.hashCode();
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
      isActive(), getParentStackVersion(), getConfigTypeAttributes());
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

  /**
   * Set the path of the stack upgrade directory.
   *
   * @param path the path to the upgrades directory
   */
  public void setUpgradesFolder(String path) {
    upgradesFolder = path;
  }

  /**
   * Obtain the path of the upgrades folder or null if directory doesn't exist.
   *
   * @return the upgrades folder, or {@code null} if not set
   */
  public String getUpgradesFolder() {
    return upgradesFolder;
  }

  /**
   * Set upgrade packs.
   *
   * @param upgradePacks  map of upgrade packs
   */
  public void setUpgradePacks(Map<String, UpgradePack> upgradePacks) {
    this.upgradePacks = upgradePacks;
  }

  /**
   * Obtain all stack upgrade packs.
   *
   * @return map of upgrade pack name to upgrade pack or {@code null} of no packs
   */
  public Map<String, UpgradePack> getUpgradePacks() {
    return upgradePacks;
  }


  @Override
  public int compareTo(StackInfo o) {
    String myId = name + "-" + version;
    String oId = o.name + "-" + o.version;
    return myId.compareTo(oId);
  }
}
