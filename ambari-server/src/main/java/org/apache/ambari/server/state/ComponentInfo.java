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
  private String cardinality = "0+";

  /**
   * Technically, no component is required to advertise a version. In practice, 
   * Components should advertise a version through a mechanism like hdp-select.
   * The version must be present the structured output.in the {"version": "#.#.#.#-###"}
   * For example, Masters will typically advertise the version upon a RESTART.
   * Whereas clients will advertise the version when INSTALLED.
   * Some components do not need to advertise a version because it is either redundant, or they don't have a mechanism
   * at the moment. For instance, ZKFC has the same version as NameNode, while AMBARI_METRICS and KERBEROS do not have a mechanism.
   */
  @XmlElements(@XmlElement(name = "versionAdvertised"))
  private boolean versionAdvertised = false;

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

  private String timelineAppid;

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
    versionAdvertised = prototype.versionAdvertised;
    clientsToUpdateConfigs = prototype.clientsToUpdateConfigs;
    commandScript = prototype.commandScript;
    customCommands = prototype.customCommands;
    dependencies = prototype.dependencies;
    autoDeploy = prototype.autoDeploy;
    configDependencies = prototype.configDependencies;
    clientConfigFiles = prototype.clientConfigFiles;
    timelineAppid = prototype.timelineAppid;
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

  public void setAutoDeploy(AutoDeployInfo autoDeploy) {
    this.autoDeploy = autoDeploy;
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

  public void setVersionAdvertised(boolean versionAdvertised) {
    this.versionAdvertised = versionAdvertised;
  }

  public boolean isVersionAdvertised() {
    return versionAdvertised;
  }

  public List<String> getClientsToUpdateConfigs() {
    return clientsToUpdateConfigs;
  }

  public void setClientsToUpdateConfigs(List<String> clientsToUpdateConfigs) {
    this.clientsToUpdateConfigs = clientsToUpdateConfigs;
  }

  public String getTimelineAppid() {
    return timelineAppid;
  }

  public void setTimelineAppid(String timelineAppid) {
    this.timelineAppid = timelineAppid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ComponentInfo that = (ComponentInfo) o;

    if (deleted != that.deleted) return false;
    if (autoDeploy != null ? !autoDeploy.equals(that.autoDeploy) : that.autoDeploy != null) return false;
    if (cardinality != null ? !cardinality.equals(that.cardinality) : that.cardinality != null) return false;
    if (versionAdvertised != that.versionAdvertised) return false;
    if (category != null ? !category.equals(that.category) : that.category != null) return false;
    if (clientConfigFiles != null ? !clientConfigFiles.equals(that.clientConfigFiles) : that.clientConfigFiles != null)
      return false;
    if (commandScript != null ? !commandScript.equals(that.commandScript) : that.commandScript != null) return false;
    if (configDependencies != null ? !configDependencies.equals(that.configDependencies) : that.configDependencies != null)
      return false;
    if (customCommands != null ? !customCommands.equals(that.customCommands) : that.customCommands != null)
      return false;
    if (dependencies != null ? !dependencies.equals(that.dependencies) : that.dependencies != null) return false;
    if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (clientConfigFiles != null ? !clientConfigFiles.equals(that.clientConfigFiles) :
        that.clientConfigFiles != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (category != null ? category.hashCode() : 0);
    result = 31 * result + (deleted ? 1 : 0);
    result = 31 * result + (cardinality != null ? cardinality.hashCode() : 0);
    result = 31 * result + (versionAdvertised ? 1 : 0);
    result = 31 * result + (commandScript != null ? commandScript.hashCode() : 0);
    result = 31 * result + (clientConfigFiles != null ? clientConfigFiles.hashCode() : 0);
    result = 31 * result + (customCommands != null ? customCommands.hashCode() : 0);
    result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
    result = 31 * result + (autoDeploy != null ? autoDeploy.hashCode() : 0);
    result = 31 * result + (configDependencies != null ? configDependencies.hashCode() : 0);
    result = 31 * result + (clientConfigFiles != null ? clientConfigFiles.hashCode() : 0);
    return result;
  }
}
