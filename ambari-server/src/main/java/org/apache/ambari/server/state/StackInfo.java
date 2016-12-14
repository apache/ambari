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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.controller.StackVersionResponse;
import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.ambari.server.state.stack.UpgradePack;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Files;

public class StackInfo implements Comparable<StackInfo>, Validable{
  private String minJdk;
  private String maxJdk;
  private String name;
  private String version;
  private String minUpgradeVersion;
  private boolean active;
  private String rcoFileLocation;
  private String kerberosDescriptorFileLocation;
  private String widgetsDescriptorFileLocation;
  private List<RepositoryInfo> repositories;
  private Collection<ServiceInfo> services;
  private Collection<ExtensionInfo> extensions;
  private String parentStackVersion;
  // stack-level properties
  private List<PropertyInfo> properties;
  private Map<String, Map<String, Map<String, String>>> configTypes;
  private Map<String, UpgradePack> upgradePacks;
  private ConfigUpgradePack configUpgradePack;
  private StackRoleCommandOrder roleCommandOrder;
  private boolean valid = true;
  private Map<String, Map<PropertyInfo.PropertyType, Set<String>>> propertiesTypesCache =
      Collections.synchronizedMap(new HashMap<String, Map<PropertyInfo.PropertyType, Set<String>>>());
  private Map<String, Map<String, Map<String, String>>> configPropertyAttributes =  null;
  /**
   * Meaning: stores subpath from stack root to exact hooks folder for stack. These hooks are
   * applied to all commands for services in current stack.
   */
  private String stackHooksFolder;
  private String upgradesFolder = null;
  private volatile Map<String, PropertyInfo> requiredProperties;
  private Map<String, VersionDefinitionXml> versionDefinitions = new ConcurrentHashMap<>();
  private Set<String> errorSet = new HashSet<String>();
  private RepositoryXml repoXml = null;

  /**
   * List of services removed from current stack
   * */
  private List<String> removedServices = new ArrayList<String>();

  public String getMinJdk() {
    return minJdk;
  }

  public void setMinJdk(String minJdk) {
    this.minJdk = minJdk;
  }

  public String getMaxJdk() {
    return maxJdk;
  }

  public void setMaxJdk(String maxJdk) {
    this.maxJdk = maxJdk;
  }

  /**
   *
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   *
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }

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

  /**
   * @return A list containing all repos for this stack, grouped by os
   */
  public ListMultimap<String, RepositoryInfo> getRepositoriesByOs() {
    return Multimaps.index(getRepositories(), RepositoryInfo.GET_OSTYPE_FUNCTION);
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

  public synchronized Collection<ExtensionInfo> getExtensions() {
    if (extensions == null) extensions = new ArrayList<ExtensionInfo>();
    return extensions;
  }

  public ExtensionInfo getExtension(String name) {
    Collection<ExtensionInfo> extensions = getExtensions();
    for (ExtensionInfo extension : extensions) {
      if (extension.getName().equals(name)) {
        return extension;
      }
    }
    //todo: exception?
    return null;
  }

  public ExtensionInfo getExtensionByService(String serviceName) {
    Collection<ExtensionInfo> extensions = getExtensions();
    for (ExtensionInfo extension : extensions) {
      for (ServiceInfo service : services) {
        if (service.getName().equals(serviceName))
          return extension;
      }
    }
    //todo: exception?
    return null;
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
      version + "\nactive:" + active + " \nvalid:" + isValid());
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

    // Get the stack-level Kerberos descriptor file path
    String stackDescriptorFileFilePath = getKerberosDescriptorFileLocation();

    // Collect the services' Kerberos descriptor files
    Collection<ServiceInfo> serviceInfos = getServices();
    // The collection of service descriptor files. A Set is being used because some Kerberos descriptor
    // files contain multiple services, therefore the same File may be encountered more than once.
    // For example the YARN directory may contain YARN and MAPREDUCE2 services.
    Collection<File> serviceDescriptorFiles = new HashSet<File>();
    if (serviceInfos != null) {
      for (ServiceInfo serviceInfo : serviceInfos) {
        File file = serviceInfo.getKerberosDescriptorFile();
        if (file != null) {
          serviceDescriptorFiles.add(file);
        }
      }
    }

    return new StackVersionResponse(getVersion(), getMinUpgradeVersion(),
        isActive(), getParentStackVersion(), getConfigTypeAttributes(),
        (stackDescriptorFileFilePath == null) ? null : new File(stackDescriptorFileFilePath),
        serviceDescriptorFiles,
        null == upgradePacks ? Collections.<String>emptySet() : upgradePacks.keySet(),
        isValid(), getErrors(), getMinJdk(), getMaxJdk());
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

  public StackRoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrder;
  }

  public void setRoleCommandOrder(StackRoleCommandOrder roleCommandOrder) {
    this.roleCommandOrder = roleCommandOrder;
  }

  public String getRcoFileLocation() {
    return rcoFileLocation;
  }

  public void setRcoFileLocation(String rcoFileLocation) {
    this.rcoFileLocation = rcoFileLocation;
  }

  /**
   * Gets the path to the stack-level Kerberos descriptor file
   *
   * @return a String containing the path to the stack-level Kerberos descriptor file
   */
  public String getKerberosDescriptorFileLocation() {
    return kerberosDescriptorFileLocation;
  }

  /**
   * Sets the path to the stack-level Kerberos descriptor file
   *
   * @param kerberosDescriptorFileLocation a String containing the path to the stack-level Kerberos
   *                                       descriptor file
   */
  public void setKerberosDescriptorFileLocation(String kerberosDescriptorFileLocation) {
    this.kerberosDescriptorFileLocation = kerberosDescriptorFileLocation;
  }

  public String getWidgetsDescriptorFileLocation() {
    return widgetsDescriptorFileLocation;
  }

  public void setWidgetsDescriptorFileLocation(String widgetsDescriptorFileLocation) {
    this.widgetsDescriptorFileLocation = widgetsDescriptorFileLocation;
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
   * Obtain all stack upgrade packs.
   *
   * @return map of upgrade pack name to upgrade pack or {@code null} if no packs
   */
  public Map<String, UpgradePack> getUpgradePacks() {
    return upgradePacks;
  }

  /**
   * Set upgrade packs.
   *
   * @param upgradePacks map of upgrade packs
   */
  public void setUpgradePacks(Map<String, UpgradePack> upgradePacks) {
    this.upgradePacks = upgradePacks;
  }

  /**
   * Get config upgrade pack for stack
   * @return config upgrade pack for stack or null if it is
   * not defined
   */
  public ConfigUpgradePack getConfigUpgradePack() {
    return configUpgradePack;
  }

  /**
   * Set config upgrade pack for stack
   * @param configUpgradePack config upgrade pack for stack or null if it is
   * not defined
   */
  public void setConfigUpgradePack(ConfigUpgradePack configUpgradePack) {
    this.configUpgradePack = configUpgradePack;
  }

  @Override
  public int compareTo(StackInfo o) {
    String myId = name + "-" + version;
    String oId = o.name + "-" + o.version;
    return myId.compareTo(oId);
  }

  //todo: ensure that required properties are never modified...
  public Map<String, PropertyInfo> getRequiredProperties() {
    Map<String, PropertyInfo> result = requiredProperties;
    if (result == null) {
      synchronized(this) {
        result = requiredProperties;
        if (result == null) {
          requiredProperties = result = new HashMap<String, PropertyInfo>();
          List<PropertyInfo> properties = getProperties();
          for (PropertyInfo propertyInfo : properties) {
            if (propertyInfo.isRequireInput()) {
              result.put(propertyInfo.getName(), propertyInfo);
            }
          }
        }
      }
    }
    return result;
  }

  public Map<PropertyInfo.PropertyType, Set<String>> getConfigPropertiesTypes(String configType) {
    if(!propertiesTypesCache.containsKey(configType)) {
      Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes = new HashMap<>();
      Collection<ServiceInfo> services = getServices();
      for (ServiceInfo serviceInfo : services) {
        for (PropertyInfo propertyInfo : serviceInfo.getProperties()) {
          if (propertyInfo.getFilename().contains(configType) && !propertyInfo.getPropertyTypes().isEmpty()) {
            Set<PropertyInfo.PropertyType> types = propertyInfo.getPropertyTypes();
            for (PropertyInfo.PropertyType propertyType : types) {
              if (!propertiesTypes.containsKey(propertyType))
                propertiesTypes.put(propertyType, new HashSet<String>());
              propertiesTypes.get(propertyType).add(propertyInfo.getName());
            }
          }
        }
      }
      propertiesTypesCache.put(configType, propertiesTypes);
    }
    return propertiesTypesCache.get(configType);
  }

  /**
   * Return default config attributes for specified config type.
   * @param configType config type
   * @return config attributes
   */
  public synchronized Map<String, Map<String, String>> getDefaultConfigAttributesForConfigType(String configType){
    if(configPropertyAttributes == null){
      configPropertyAttributes = getDefaultConfigAttributes();
    }
    if(configPropertyAttributes.containsKey(configType)){
      return configPropertyAttributes.get(configType);
    }
    return null;
  }

  private Map<String,  Map<String, Map<String, String>>> getDefaultConfigAttributes(){
    Map<String,  Map<String, Map<String, String>>> result = new HashMap<>();
    for(ServiceInfo si : services){
      for(PropertyInfo pi : si.getProperties())
      {
        String propertyConfigType = Files.getNameWithoutExtension(pi.getFilename());
        String propertyName = pi.getName();
        String hidden = pi.getPropertyValueAttributes().getHidden();
        if(hidden != null){
          if(!result.containsKey(propertyConfigType)){
            result.put(propertyConfigType, new HashMap<String, Map<String, String>>());
          }
          if(!result.get(propertyConfigType).containsKey("hidden")){
            result.get(propertyConfigType).put("hidden", new HashMap<String, String>());
          }
          result.get(propertyConfigType).get("hidden").put(propertyName, hidden);
        }
      }
    }
    return result;
  }

  /**
   * @param key the version that the xml represents
   * @param xml the version definition object
   */
  public void addVersionDefinition(String key, VersionDefinitionXml xml) {
    versionDefinitions.put(key, xml);
  }

  /**
   * @return the list of available definitions on this stack
   */
  public Collection<VersionDefinitionXml> getVersionDefinitions() {
    return versionDefinitions.values();
  }

  /**
   * @param rxml  the repository xml object
   */
  public void setRepositoryXml(RepositoryXml rxml) {
    repoXml = rxml;
  }

  /**
   * @return the repository xml object, or {@code null} if it couldn't be loaded
   */
  public RepositoryXml getRepositoryXml() {
    return repoXml;
  }

  public List<String> getRemovedServices() {
    return removedServices;
  }

  public void setRemovedServices(List<String> removedServices) {
    this.removedServices = removedServices;
  }
}
