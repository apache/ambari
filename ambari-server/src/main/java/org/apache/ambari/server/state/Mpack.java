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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.ambari.server.stack.RepoUtil;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the state of an mpack.
 */
public class Mpack {

  /**
   * Mpack DB Id
   */
  private Long resourceId;

  private Long registryId;

  /**
   * Mpack id as defined in mpack.json
   */
  @SerializedName("id")
  private String mpackId;

  @SerializedName("name")
  private String name;

  @SerializedName("version")
  private String version;

  @SerializedName("prerequisites")
  private Map<String, String> prerequisites;

  @SerializedName("modules")
  private List<Module> modules;

  @SerializedName("definition")
  private String definition;

  @SerializedName("description")
  private String description;

  @SerializedName("displayName")
  private String displayName;

  @SerializedName("osSpecifics")
  private List<MpackOsSpecific> osSpecifics;

  private String mpackUri;

  private HashMap<String, Module> moduleHashMap;

  /**
   * The {@link RepoUtil#REPOSITORY_FILE_NAME} representation.
   */
  private RepositoryXml repositoryXml;

  public Long getResourceId() {
    return resourceId;
  }

  public void setResourceId(Long resourceId) {
    this.resourceId = resourceId;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public String getMpackId() {
    return mpackId;
  }

  public void setMpackId(String mpackId) {
    this.mpackId = mpackId;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(Map<String, String> prerequisites) {
    this.prerequisites = prerequisites;
  }

  public List<Module> getModules() {
    return modules;
  }

  public void setModules(List<Module> modules) {
    this.modules = modules;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  public List<MpackOsSpecific> getOsSpecifics() {
    return osSpecifics;
  }

  public void setOsSpecifics(List<MpackOsSpecific> osSpecifics) {
    this.osSpecifics = osSpecifics;
  }

  /**
   * Gets the repository XML representation.
   *
   * @return the {@link RepoUtil#REPOSITORY_FILE_NAME} unmarshalled.
   */
  public RepositoryXml getRepositoryXml() {
    return repositoryXml;
  }

  /**
   * Gets the repository XML representation.
   *
   * @param repositoryXml
   *          the {@link RepoUtil#REPOSITORY_FILE_NAME} unmarshalled.
   */
  public void setRepositoryXml(RepositoryXml repositoryXml) {
    this.repositoryXml = repositoryXml;
  }

  /**
   * Gets the module with the given name. Module names are service names.
   *
   * @param moduleName
   *          the name of the module.
   * @return the module or {@code null}.
   */
  public Module getModule(String moduleName) {
     return moduleHashMap.get(moduleName);
  }

  /**
   * Gets a component from a given module.
   *
   * @param moduleName
   *          the module (service) name.
   * @param moduleComponentName
   *          the name of the component.
   * @return the component or {@code null}.
   */
  public ModuleComponent getModuleComponent(String moduleName, String moduleComponentName) {
   Module module = moduleHashMap.get(moduleName);
      if(module !=null){
        return module.getModuleComponent(moduleComponentName);
      }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Mpack that = (Mpack) o;
    EqualsBuilder equalsBuilder = new EqualsBuilder();
    equalsBuilder.append(resourceId, that.resourceId);
    equalsBuilder.append(registryId, that.registryId);
    equalsBuilder.append(mpackId, that.mpackId);
    equalsBuilder.append(name, that.name);
    equalsBuilder.append(version, that.version);
    equalsBuilder.append(prerequisites, that.prerequisites);
    equalsBuilder.append(modules, that.modules);
    equalsBuilder.append(definition, that.definition);
    equalsBuilder.append(description, that.description);
    equalsBuilder.append(mpackUri, that.mpackUri);
    equalsBuilder.append(displayName, that.displayName);

    return equalsBuilder.isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(resourceId, registryId, mpackId, name, version, prerequisites, modules,
        definition, description, mpackUri, displayName);
  }

  @Override
  public String toString() {
    return "Mpack{" +
            "id=" + resourceId +
            ", registryId=" + registryId +
            ", mpackId='" + mpackId + '\'' +
            ", name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", prerequisites=" + prerequisites +
            ", modules=" + modules +
            ", definition='" + definition + '\'' +
            ", description='" + description + '\'' +
            ", mpackUri='" + mpackUri + '\'' +
            ", displayName='" + mpackUri + '\'' +
            '}';
  }

  public void copyFrom(Mpack mpack) {
    if (resourceId == null) {
      resourceId = mpack.getResourceId();
    }
    if (name == null) {
      name = mpack.getName();
    }
    if (mpackId == null) {
      mpackId = mpack.getMpackId();
    }
    if (version == null) {
      version = mpack.getVersion();
    }
    if (registryId == null) {
      registryId = mpack.getRegistryId();
    }
    if (description == null) {
      description = mpack.getDescription();
    }
    if (modules == null) {
      modules = mpack.getModules();
    }
    if (prerequisites == null) {
      prerequisites = mpack.getPrerequisites();
    }
    if (definition == null) {
      definition = mpack.getDefinition();
    }
    if (displayName == null) {
      displayName = mpack.getDisplayName();
    }
  }


  /**
   * Load modules in an mpack as (modulename, module)
   * Each module will have a componentMap (componentName, component)
   */
  public void populateModuleMap() {
    moduleHashMap = new HashMap<>();
    for(Module module : this.modules){
      module.populateComponentMap();
      moduleHashMap.put(module.getName(), module);
    }
  }
}
