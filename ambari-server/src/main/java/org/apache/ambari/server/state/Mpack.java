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
import java.util.Set;

import org.apache.ambari.server.stack.RepoUtil;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
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
   * Gets the stack ID of this mpack, which is a combination of name and
   * version.
   *
   * @return the stack ID of the mpack.
   */
  public StackId getStackId() {
    return new StackId(name, version);
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
    if (module != null) {
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
    return MoreObjects.toStringHelper(this)
        .add("id", resourceId)
        .add("registryId", registryId)
        .add("mpackId", mpackId)
        .add("name", name)
        .add("version", version)
        .add("displayName", displayName).toString();
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
    for(Module module : modules){
      module.populateComponentMap();
      moduleHashMap.put(module.getName(), module);
    }
  }

  /**
   * Gets a summary of the version changes between two mpacks. This will not
   * look at things like mpack descriptions, URLs, etc. It only returns
   * additions, removals, and version changes of the module components.
   *
   * @param other
   *          the mpack to compare with.
   * @return a summary of changes.
   */
  public MpackChangeSummary getChangeSummary(Mpack other) {
    MpackChangeSummary summary = new MpackChangeSummary(this, other);
    return summary;
  }

  /**
   * Contains an aggregated summary of changes in module and component version
   * from one mpack to another. This will only represent version changes between
   * components (or additions and removals).
   */
  public static class MpackChangeSummary {
    private Set<ModuleComponent> m_added = Sets.newLinkedHashSet();
    private Set<ModuleComponent> m_removed = Sets.newLinkedHashSet();
    private Set<ModuleVersionChange> m_moduleVersionChanges = Sets.newLinkedHashSet();
    private Set<ModuleComponentVersionChange> m_componentVersionChanges = Sets.newLinkedHashSet();

    private final Mpack m_source;
    private final Mpack m_target;
    
    private UpgradePack m_upgradePack = null;

    /**
     * Constructor.
     *
     * @param source
     *          the source mpack to diff from.
     * @param other
     *          the mpack to diff against.
     */
    public MpackChangeSummary(Mpack source, Mpack target) {
      m_source = source;
      m_target = target;

      for (Module module : source.getModules()) {
        Module otherModule = target.getModule(module.getName());
        if (null == otherModule) {
          module.getComponents().stream().peek(moduleComponent -> removed(moduleComponent));

          continue;
        }

        // no change in module version, no components changed
        if (StringUtils.equals(module.getVersion(), otherModule.getVersion())) {
          continue;
        }

        // module version changed
        ModuleVersionChange moduleVersionChange = changed(module, otherModule);

        for (ModuleComponent moduleComponent : module.getComponents()) {
          ModuleComponent otherComponent = module.getModuleComponent(moduleComponent.getName());
          if (null == otherComponent) {
            removed(moduleComponent);
            continue;
          }

          // module component version changed
          if (!StringUtils.equals(moduleComponent.getVersion(), otherComponent.getVersion())) {
            changed(moduleVersionChange, moduleComponent, otherComponent);
          }
        }
      }

      for (Module otherModule : target.getModules()) {
        Module thisModule = source.getModule(otherModule.getName());
        if (null == thisModule) {
          otherModule.getComponents().stream().peek(moduleComponent -> added(moduleComponent));

          continue;
        }

        for (ModuleComponent otherComponent : otherModule.getComponents()) {
          ModuleComponent thisComponent = thisModule.getModuleComponent(otherComponent.getName());
          if (null == thisComponent) {
            added(otherComponent);
          }
        }
      }
    }

    /**
     * Gets whether there are any changes represented by this summary.
     */
    public boolean hasChanges() {
      return !(m_added.isEmpty() && m_removed.isEmpty() && m_moduleVersionChanges.isEmpty());
    }

    /**
     * A module component was added between mpacks.
     *
     * @param moduleComponent
     *          the added module component.
     */
    public void added(ModuleComponent moduleComponent) {
      m_added.add(moduleComponent);
    }

    /**
     * A module was changed between mpacks.
     *
     * @param moduleComponent
     *          the removed module component.
     */
    public ModuleVersionChange changed(Module source, Module target) {
      ModuleVersionChange change = new ModuleVersionChange(source, target);
      m_moduleVersionChanges.add(change);
      return change;
    }

    /**
     * A module component was changed between mpacks.
     *
     * @param moduleVersionChange
     *          the parent module change for this component.
     * @param source
     *          the source component version
     * @param target
     *          the target component version
     */
    public ModuleComponentVersionChange changed(ModuleVersionChange moduleVersionChange,
        ModuleComponent source, ModuleComponent target) {
      ModuleComponentVersionChange change = new ModuleComponentVersionChange(source, target);
      m_componentVersionChanges.add(change);

      moduleVersionChange.m_componentChanges.add(change);
      return change;
    }

    /**
     * A module component had a version changed between mpacks.
     *
     * @param moduleComponent
     */
    public void removed(ModuleComponent moduleComponent) {
      m_removed.add(moduleComponent);
    }

    /**
     * @return the added
     */
    public Set<ModuleComponent> getAdded() {
      return m_added;
    }

    /**
     * @return the removed
     */
    public Set<ModuleComponent> getRemoved() {
      return m_removed;
    }

    /**
     * Gets all modules which have changed versions.
     *
     * @return the changed modules
     */
    public Set<ModuleVersionChange> getModuleVersionChanges() {
      return m_moduleVersionChanges;
    }

    /**
     * Gets all components which have changed versions.
     *
     * @return the changed modules.
     */
    public Set<ModuleComponentVersionChange> getComponentVersionChanges() {
      return m_componentVersionChanges;
    }

    /**
     * Gets the source mpack the diff is from.
     *
     * @return the source
     */
    public Mpack getSource() {
      return m_source;
    }

    /**
     * Gets the mpack which was diff'd against.
     *
     * @return the other
     */
    public Mpack getTarget() {
      return m_target;
    }

    /**
     * Gets whether there are version changes of module components between
     * mpacks.
     *
     * @return
     */
    public boolean hasVersionChanges() {
      return !m_moduleVersionChanges.isEmpty();
    }

    /**
     * Sets the upgrade pack which this summary uses for orchestration.
     * 
     * @param upgradePack
     *          the upgrade pack
     */
    public void setUpgradePack(UpgradePack upgradePack) {
      m_upgradePack = upgradePack;
    }
    
    /**
     * @return the upgrade pack used for orchestration
     */
    public UpgradePack getUpgradePack() {
      return m_upgradePack;
    }
  }

  /**
   * Represents a change in module versions.
   */
  public static class ModuleVersionChange {
    private final Module m_source;
    private final Module m_target;
    private final Set<ModuleComponentVersionChange> m_componentChanges = Sets.newLinkedHashSet();

    /**
     * Constructor.
     *
     * @param source
     * @param target
     */
    public ModuleVersionChange(Module source, Module target) {
      m_source = source;
      m_target = target;
    }

    public Module getSource() {
      return m_source;
    }

    public Module getTarget() {
      return m_target;
    }

    public Set<ModuleComponentVersionChange> getComponentChanges() {
      return m_componentChanges;
    }
  }

  /**
   * Represents a change in module component versions.
   */
  public static class ModuleComponentVersionChange {
    private final ModuleComponent m_source;
    private final ModuleComponent m_target;

    /**
     * Constructor.
     *
     * @param source
     * @param target
     */
    public ModuleComponentVersionChange(ModuleComponent source, ModuleComponent target) {
      m_source = source;
      m_target = target;
    }

    public ModuleComponent getSource() {
      return m_source;
    }

    public ModuleComponent getTarget() {
      return m_target;
    }
  }
}
