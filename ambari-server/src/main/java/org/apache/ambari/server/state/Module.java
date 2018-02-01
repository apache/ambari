/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Module {
  public enum Category {
    @SerializedName("SERVER")
    SERVER,
    @SerializedName("CLIENT")
    CLIENT,
    @SerializedName("LIBRARY")
    LIBRARY
  }

  @SerializedName("id")
  private String id;
  @SerializedName("displayName")
  private String displayName;
  @SerializedName("description")
  private String description;
  @SerializedName("category")
  private Category category;
  @SerializedName("name")
  private String name;
  @SerializedName("version")
  private String version;
  @SerializedName("definition")
  private String definition;
  @SerializedName("dependencies")
  private List<ModuleDependency> moduleDependencyList;
  @SerializedName("components")
  private List<ModuleComponent> moduleComponentList;

  public Category getCategory() {
    return category;
  }

  public void setType(Category category) {
    this.category = category;
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

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public List<ModuleDependency> getModuleDependencyList() {
    return moduleDependencyList;
  }

  public void setModuleDependencyList(List<ModuleDependency> moduleDependencyList) {
    this.moduleDependencyList = moduleDependencyList;
  }

  public List<ModuleComponent> getModuleComponentList() {
    return moduleComponentList;
  }

  public void setModuleComponentList(List<ModuleComponent> moduleComponentList) {
    this.moduleComponentList = moduleComponentList;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Module module = (Module) o;

    if (!id.equals(module.id)) return false;
    if (!displayName.equals(module.displayName)) return false;
    if (!description.equals(module.description)) return false;
    if (category != module.category) return false;
    if (!name.equals(module.name)) return false;
    if (!version.equals(module.version)) return false;
    if (!definition.equals(module.definition)) return false;
    if (moduleDependencyList != null ? !moduleDependencyList.equals(module.moduleDependencyList) : module.moduleDependencyList != null)
      return false;
    return moduleComponentList.equals(module.moduleComponentList);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + displayName.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + category.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + definition.hashCode();
    result = 31 * result + (moduleDependencyList != null ? moduleDependencyList.hashCode() : 0);
    result = 31 * result + moduleComponentList.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Module{" +
            "id='" + id + '\'' +
            ", displayName='" + displayName + '\'' +
            ", description='" + description + '\'' +
            ", category=" + category +
            ", name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", definition='" + definition + '\'' +
            ", moduleDependencyList=" + moduleDependencyList +
            ", moduleComponentList=" + moduleComponentList +
            '}';
  }
}
