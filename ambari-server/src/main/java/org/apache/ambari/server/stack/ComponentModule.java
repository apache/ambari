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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.DependencyInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Component module which provides all functionality related to parsing and fully
 * resolving service components from the stack definition.
 */
public class ComponentModule extends BaseModule<ComponentModule, ComponentInfo> implements Validable {
  /**
   * Corresponding component info
   */
  private ComponentInfo componentInfo;
  
  /**
   * validity flag
   */
  protected boolean valid = true;

          
  /**
   * Constructor.
   *
   * @param componentInfo  associated component info
   */
  public ComponentModule(ComponentInfo componentInfo) {
    this.componentInfo = componentInfo;
  }

  @Override
  public void resolve(ComponentModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices) {
    if (parent != null) {
      ComponentInfo parentInfo = parent.getModuleInfo();
      if (!parent.isValid()) {
        setValid(false);
        setErrors(parent.getErrors());
      }

      if (componentInfo.getCommandScript() == null) {
        componentInfo.setCommandScript(parentInfo.getCommandScript());
      }
      if (componentInfo.getDisplayName() == null) {
        componentInfo.setDisplayName(parentInfo.getDisplayName());
      }
      if (componentInfo.getConfigDependencies() == null) {
        componentInfo.setConfigDependencies(parentInfo.getConfigDependencies());
      }
      if (componentInfo.getClientConfigFiles() == null) {
        componentInfo.setClientConfigFiles(parentInfo.getClientConfigFiles());
      }
      if (componentInfo.getClientsToUpdateConfigs() == null) {
        componentInfo.setClientsToUpdateConfigs(parentInfo.getClientsToUpdateConfigs());
      }
      if (componentInfo.getCategory() == null) {
        componentInfo.setCategory(parentInfo.getCategory());
      }
      if (componentInfo.getCardinality() == null) {
        componentInfo.setCardinality(parentInfo.getCardinality());
      }
      componentInfo.setVersionAdvertised(parentInfo.isVersionAdvertised());
      if (componentInfo.getAutoDeploy() == null) {
        componentInfo.setAutoDeploy(parentInfo.getAutoDeploy());
      }

      mergeComponentDependencies(parentInfo.getDependencies(),
              componentInfo.getDependencies());

      mergeCustomCommands(parentInfo.getCustomCommands(),
              componentInfo.getCustomCommands());
    }
  }

  @Override
  public ComponentInfo getModuleInfo() {
    return componentInfo;
  }

  @Override
  public boolean isDeleted() {
    return componentInfo.isDeleted();
  }

  @Override
  public String getId() {
    return componentInfo.getName();
  }

  /**
   * Merge component dependencies.
   * Child dependencies override a parent dependency of the same name.
   *
   * @param parentDependencies  parent dependencies
   * @param childDependencies   child dependencies
   */
  //todo: currently there is no way to remove an inherited dependency
  private void mergeComponentDependencies(List<DependencyInfo> parentDependencies,
                                          List<DependencyInfo> childDependencies) {

    Collection<String> existingNames = new HashSet<String>();

    for (DependencyInfo childDependency : childDependencies) {
      existingNames.add(childDependency.getName());
    }
    if (parentDependencies != null) {
      for (DependencyInfo parentDependency : parentDependencies) {
        if (! existingNames.contains(parentDependency.getName())) {
          childDependencies.add(parentDependency);
        }
      }
    }
  }

  /**
   * Merge custom commands.
   * Child commands override a parent command of the same name.
   *
   * @param parentCommands  parent commands
   * @param childCommands   child commands
   */
  //todo: duplicated in ServiceModule
  //todo: currently there is no way to remove an inherited custom command
  private void mergeCustomCommands(List<CustomCommandDefinition> parentCommands,
                                   List<CustomCommandDefinition> childCommands) {

    Collection<String> existingNames = new HashSet<String>();

    for (CustomCommandDefinition childCmd : childCommands) {
      existingNames.add(childCmd.getName());
    }
    if (parentCommands != null) {
      for (CustomCommandDefinition parentCmd : parentCommands) {
        if (! existingNames.contains(parentCmd.getName())) {
          childCommands.add(parentCmd);
        }
      }
    }
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  }
  
  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
  }  
}
