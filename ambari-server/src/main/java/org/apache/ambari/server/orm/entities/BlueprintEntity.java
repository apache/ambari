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

package org.apache.ambari.server.orm.entities;

import com.google.gson.Gson;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.PropertyInfo;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Entity representing a Blueprint.
 */
@Table(name = "blueprint")
@NamedQuery(name = "allBlueprints",
    query = "SELECT blueprint FROM BlueprintEntity blueprint")
@Entity
public class BlueprintEntity {

  @Id
  @Column(name = "blueprint_name", nullable = false, insertable = true,
      updatable = false, unique = true, length = 100)
  private String blueprintName;

  @Column(name = "stack_name", nullable = false, insertable = true, updatable = false)
  @Basic
  private String stackName;

  @Column(name = "stack_version", nullable = false, insertable = true, updatable = false)
  @Basic
  private String stackVersion;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "blueprint")
  private Collection<HostGroupEntity> hostGroups;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "blueprint")
  private Collection<BlueprintConfigEntity> configurations;

  @Transient
  private Gson jsonSerializer = new Gson();


  /**
   * Get the blueprint name.
   *
   * @return blueprint name
   */
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the blueprint name
   *
   * @param blueprintName  the blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Get the stack name.
   *
   * @return the stack name
   */
  public String getStackName() {
    return stackName;
  }

  /**
   * Set the stack name.
   *
   * @param stackName  the stack name
   */
  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  /**
   * Get the stack version.
   *
   * @return the stack version
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * Set the stack version.
   *
   * @param stackVersion the stack version
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  /**
   * Get the collection of associated host groups.
   *
   * @return collection of host groups
   */
  public Collection<HostGroupEntity> getHostGroups() {
    return hostGroups;
  }

  /**
   * Set the host group collection.
   *
   * @param hostGroups  collection of associated host groups
   */
  public void setHostGroups(Collection<HostGroupEntity> hostGroups) {
    this.hostGroups = hostGroups;
  }

  /**
   * Get the collection of associated configurations.
   *
   * @return collection of configurations
   */
  public Collection<BlueprintConfigEntity> getConfigurations() {
    return configurations;
  }

  /**
   * Set the configuration collection.
   *
   * @param configurations  collection of associated configurations
   */
  public void setConfigurations(Collection<BlueprintConfigEntity> configurations) {
    this.configurations = configurations;
  }

  /**
   * Validate all configurations.  Validation is done on the operational configuration of each
   * host group.  An operational configuration is achieved by overlaying host group configuration
   * on top of cluster configuration which overlays the default stack configurations.
   *
   * @param stackInfo  stack information
   * @param type       type of required property to check (PASSWORD|DEFAULT)
   * @return map of required properties which are missing.  Empty map if none are missing.
   *
   * @throws IllegalArgumentException if blueprint contains invalid information
   */
  public Map<String, Map<String, Collection<String>>> validateConfigurations(
      AmbariMetaInfo stackInfo, boolean validatePasswords) {

    String stackName = getStackName();
    String stackVersion = getStackVersion();

    Map<String, Map<String, Collection<String>>> missingProperties =
        new HashMap<String, Map<String, Collection<String>>>();
    Map<String, Map<String, String>> clusterConfigurations = getConfigurationAsMap(getConfigurations());

    for (HostGroupEntity hostGroup : getHostGroups()) {
      Collection<String> processedServices = new HashSet<String>();
      Map<String, Collection<String>> allRequiredProperties = new HashMap<String, Collection<String>>();
      Map<String, Map<String, String>> operationalConfiguration =
          new HashMap<String, Map<String, String>>(clusterConfigurations);

      operationalConfiguration.putAll(getConfigurationAsMap(hostGroup.getConfigurations()));
      for (HostGroupComponentEntity component : hostGroup.getComponents()) {
        //for now, AMBARI is not recognized as a service in Stacks
        if (! component.getName().equals("AMBARI_SERVER")) {
          String service;
          try {
            service = stackInfo.getComponentToService(stackName, stackVersion, component.getName());
          } catch (AmbariException e) {
            throw new IllegalArgumentException("Unable to determine the service associated with the" +
                " component: " + component.getName());
          }
          if (processedServices.add(service)) {
            Map<String, PropertyInfo> serviceRequirements = stackInfo.getRequiredProperties(
                stackName, stackVersion, service);

            for (PropertyInfo propertyInfo : serviceRequirements.values()) {
              if (! (validatePasswords ^ propertyInfo.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD))) {
                String configCategory = propertyInfo.getFilename();
                if (configCategory.endsWith(".xml")) {
                  configCategory = configCategory.substring(0, configCategory.indexOf(".xml"));
                }
                Collection<String> typeRequirements = allRequiredProperties.get(configCategory);
                if (typeRequirements == null) {
                  typeRequirements = new HashSet<String>();
                  allRequiredProperties.put(configCategory, typeRequirements);
                }
                typeRequirements.add(propertyInfo.getName());
              }
            }
          }
        }
      }
      for (Map.Entry<String, Collection<String>> requiredTypeProperties : allRequiredProperties.entrySet()) {
        String requiredCategory = requiredTypeProperties.getKey();
        Collection<String> requiredProperties = requiredTypeProperties.getValue();
        Collection<String> operationalTypeProps = operationalConfiguration.containsKey(requiredCategory) ?
            operationalConfiguration.get(requiredCategory).keySet() :
            Collections.<String>emptyList();

        requiredProperties.removeAll(operationalTypeProps);
        if (! requiredProperties.isEmpty()) {
          String hostGroupName = hostGroup.getName();
          Map<String, Collection<String>> hostGroupMissingProps = missingProperties.get(hostGroupName);
          if (hostGroupMissingProps == null) {
            hostGroupMissingProps = new HashMap<String, Collection<String>>();
            missingProperties.put(hostGroupName, hostGroupMissingProps);
          }
          hostGroupMissingProps.put(requiredCategory, requiredProperties);
        }
      }
    }
    return missingProperties;
  }

  /**
   * Obtain configuration as a map of config type to corresponding properties.
   *
   * @param configurations  configuration to include in map
   *
   * @return map of config type to map of properties
   */
  private Map<String, Map<String, String>> getConfigurationAsMap(
      Collection<? extends BlueprintConfiguration> configurations) {

    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    for (BlueprintConfiguration config : configurations) {
      String type = config.getType();
      Map<String, String> typeProperties = jsonSerializer.<Map<String, String>>fromJson(
          config.getConfigData(), Map.class);
      properties.put(type, typeProperties);
    }
    return properties;
  }
}
