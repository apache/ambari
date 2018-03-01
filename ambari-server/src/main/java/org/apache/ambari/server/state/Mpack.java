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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.stack.RepoUtil;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.commons.lang.StringUtils;

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

  private String mpackUri;

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
    for (Module module : modules) {
      if (StringUtils.equals(moduleName, module.getName())) {
        return module;
      }
    }

    return null;
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
    for( Module module : modules ) {
      ModuleComponent moduleComponent = module.getModuleComponent(moduleComponentName);
      if( null != moduleComponent ) {
        return moduleComponent;
      }
    }

    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Mpack mpack = (Mpack) o;

    if (!resourceId.equals(mpack.resourceId)) {
      return false;
    }
    if (registryId != null ? !registryId.equals(mpack.registryId) : mpack.registryId != null) {
      return false;
    }
    if (!mpackId.equals(mpack.mpackId)) {
      return false;
    }
    if (!name.equals(mpack.name)) {
      return false;
    }
    if (!version.equals(mpack.version)) {
      return false;
    }
    if (!prerequisites.equals(mpack.prerequisites)) {
      return false;
    }
    if (!modules.equals(mpack.modules)) {
      return false;
    }
    if (!definition.equals(mpack.definition)) {
      return false;
    }
    if (!description.equals(mpack.description)) {
      return false;
    }
    return mpackUri.equals(mpack.mpackUri);
  }

  @Override
  public int hashCode() {
    int result = resourceId.hashCode();
    result = 31 * result + (registryId != null ? registryId.hashCode() : 0);
    result = 31 * result + mpackId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + prerequisites.hashCode();
    result = 31 * result + modules.hashCode();
    result = 31 * result + definition.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + mpackUri.hashCode();
    return result;
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
  }
}
