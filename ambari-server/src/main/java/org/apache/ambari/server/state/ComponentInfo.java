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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComponentInfo {
  private String name;
  private String displayName;
  private String category;
  private boolean deleted;
  private String cardinality;

  /**
  * Added at schema ver 2
  */
  private CommandScriptDefinition commandScript;

  /**
   * List of clients which configs are updated with master component.
   * If clientsToUpdateConfigs is not specified all clients are considered to be updated.
   * If clientsToUpdateConfigs is empty no clients are considered to be updated
   */
  @XmlElementWrapper(name = "clientsToUpdateConfigs")
  @XmlElements(@XmlElement(name = "client"))
  private List<String> clientsToUpdateConfigs;

  /**
   * Client configuration files
   * List of files to download in client configuration tar
   */
  @XmlElementWrapper(name = "configFiles")
  @XmlElements(@XmlElement(name = "configFile"))
  private List<ClientConfigFileDefinition> clientConfigFiles;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name="customCommands")
  @XmlElements(@XmlElement(name="customCommand"))
  private List<CustomCommandDefinition> customCommands;

  /**
   * Component dependencies to other components.
   */
  @XmlElementWrapper(name="dependencies")
  @XmlElements(@XmlElement(name="dependency"))
  private List<DependencyInfo> dependencies = new ArrayList<DependencyInfo>();

  /**
   * Auto-deployment information.
   * If auto-deployment is enabled and the component doesn't meet the cardinality requirement,
   * the component is auto-deployed to the cluster topology.
   */
  @XmlElement(name="auto-deploy")
  private AutoDeployInfo autoDeploy;

  public ComponentInfo() {
  }

  /**
   * Copy constructor.
   */
  public ComponentInfo(ComponentInfo prototype) {
    name = prototype.name;
    category = prototype.category;
    deleted = prototype.deleted;
    cardinality = prototype.cardinality;
    clientsToUpdateConfigs = prototype.clientsToUpdateConfigs;
    commandScript = prototype.commandScript;
    customCommands = prototype.customCommands;
    dependencies = prototype.dependencies;
    autoDeploy = prototype.autoDeploy;
    configDependencies = prototype.configDependencies;
    clientConfigFiles = prototype.clientConfigFiles;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isClient() {
    return "CLIENT".equals(category);
  }

  public boolean isMaster() {
    return "MASTER".equals(category);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  public void setCommandScript(CommandScriptDefinition commandScript) {
    this.commandScript = commandScript;
  }

  public List<ClientConfigFileDefinition> getClientConfigFiles() {
    return clientConfigFiles;
  }

  public void setClientConfigFiles(List<ClientConfigFileDefinition> clientConfigFiles) {
    this.clientConfigFiles = clientConfigFiles;
  }

  public List<CustomCommandDefinition> getCustomCommands() {
    if (customCommands == null) {
      customCommands = new ArrayList<CustomCommandDefinition>();
    }
    return customCommands;
  }

  public void setCustomCommands(List<CustomCommandDefinition> customCommands) {
    this.customCommands = customCommands;
  }

  public boolean isCustomCommand(String commandName) {
    if (customCommands != null && commandName != null) {
      for (CustomCommandDefinition cc: customCommands) {
        if (commandName.equals(cc.getName())){
          return true;
        }
      }
    }
    return false;
  }
  public CustomCommandDefinition getCustomCommandByName(String commandName){
    for(CustomCommandDefinition ccd : getCustomCommands()){
      if (ccd.getName().equals(commandName)){
        return ccd;
      }
    }
    return null;
  }

  public List<DependencyInfo> getDependencies() {
    return dependencies;
  }
  @XmlElementWrapper(name="configuration-dependencies")
  @XmlElements(@XmlElement(name="config-type"))
  private List<String> configDependencies;
  

  public List<String> getConfigDependencies() {
    return configDependencies;
  }
  
  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }
  public boolean hasConfigType(String type) {
    return configDependencies != null && configDependencies.contains(type);
  }

  public void setDependencies(List<DependencyInfo> dependencies) {
    this.dependencies = dependencies;
  }

  public AutoDeployInfo getAutoDeploy() {
    return autoDeploy;
  }

  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }

  public String getCardinality() {
    return cardinality;
  }

  public List<String> getClientsToUpdateConfigs() {
    return clientsToUpdateConfigs;
  }

  public void setClientsToUpdateConfigs(List<String> clientsToUpdateConfigs) {
    this.clientsToUpdateConfigs = clientsToUpdateConfigs;
  }
}
