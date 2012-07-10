/*
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
package org.apache.ambari.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ComponentDefinition;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.common.rest.entities.ConfigurationCategory;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.common.rest.entities.RepositoryKind;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.UserGroup;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.ComponentPluginFactory;

import com.google.inject.Inject;

/**
 * This class flattens a stack and its ancestors into a single stack. The 
 * resulting stack has a client configuration at the top and a fully expanded
 * configuration for each of the roles. The configuration at the components 
 * level (other than being pushed down into the appropriate roles) is also
 * removed. Finally the "ambari" category is removed from the role configs.
 */
public class StackFlattener {

  private static final String META_CATEGORY = "ambari";
  
  private final Stacks stacks;
  private final ComponentPluginFactory plugins;

  private UserGroup flattenUserGroup(List<Stack> stacks) {
      UserGroup default_user_group = null;
      for(int i=stacks.size()-1; i>=0; --i) {
          Stack stack = stacks.get(i);
          default_user_group = stack.getDefault_user_group();
          if (default_user_group == null) {
              continue; 
          }
      }
      return default_user_group;
  }
  
  private List<RepositoryKind> flattenRepositories(List<Stack> stacks) {
    Map<String, List<String>> repositories = 
        new TreeMap<String, List<String>>();
    for(int i=stacks.size()-1; i>=0; --i) {
      Stack stack = stacks.get(i);
      List<RepositoryKind> kindList = stack.getPackageRepositories();
      if (kindList != null) {
        for(RepositoryKind kind: kindList) {
          List<String> list = repositories.get(kind.getKind());
          if (list == null) {
            list = new ArrayList<String>();
            repositories.put(kind.getKind(), list);
          }
          list.addAll(kind.getUrls());
        }
      }
    }
    
    // translate it into a list of repositorykinds
    List<RepositoryKind> result = new ArrayList<RepositoryKind>();
    for(Map.Entry<String, List<String>> item: repositories.entrySet()) {
      RepositoryKind kind = new RepositoryKind();
      kind.setKind(item.getKey());
      kind.setUrls(item.getValue());
      result.add(kind);
    }
    return result;
  }

  /**
   * Build the list of stacks in the inheritance tree.
   * The base stack is at the front of the list and the final one is last
   * @param stackName the name of the final stack
   * @param stackRevision the revision of the final stack
   * @return the lists of stacks
   * @throws IOException 
   * @throws WebApplicationException 
   */
  private List<Stack> getStackList(String stackName, 
                                   int stackRevision
                                   ) throws WebApplicationException, 
                                            IOException {
    Stack stack = stacks.getStack(stackName, stackRevision);
    List<Stack> result = new ArrayList<Stack>();
    while (stack != null) {
      result.add(0, stack);
      String parentName = stack.getParentName();
      int parentRev = stack.getParentRevision();
      if (parentName != null) {
        stack = stacks.getStack(parentName, parentRev);
      } else {
        stack = null;
      }
    }
    return result;
  }

  private Set<String> getComponents(List<Stack> stacks) {
    Set<String> result = new TreeSet<String>();
    for(Stack stack: stacks) {
      for(Component comp: stack.getComponents()) {
        result.add(comp.getName());
      }
    }
    return result;
  }
  
  /**
   * Merge the given Configuration into the map that we are building.
   * @param map the map to update
   * @param conf the configuration to merge in
   * @param dropMeta should we drop the meta category
   */
  private void mergeInConfiguration(Map<String, Map<String, Property>> map,
                                    Configuration conf, boolean dropMeta) {
    if (conf != null) {
      for (ConfigurationCategory category: conf.getCategory()) {
        if (!dropMeta || !META_CATEGORY.equals(category.getName())) {
          Map<String, Property> categoryMap = map.get(category.getName());
          if (categoryMap == null) {
            categoryMap = new TreeMap<String, Property>();
            map.put(category.getName(), categoryMap);
          }
          for (Property prop: category.getProperty()) {
            categoryMap.put(prop.getName(), prop);
          }
        }
      }
    }
  }

  private Configuration buildConfig(Map<String, Map<String, Property>> map) {
    Configuration conf = new Configuration();
    List<ConfigurationCategory> categories = conf.getCategory();
    for(String categoryName: map.keySet()) {
      ConfigurationCategory category = new ConfigurationCategory();
      categories.add(category);
      category.setName(categoryName);
      List<Property> properties = category.getProperty();
      for (Property property: map.get(categoryName).values()) {
        properties.add(property);
      }
    }
    return conf;
  }
  
  private Configuration buildClientConfiguration(List<Stack> stacks) {
    Map<String, Map<String, Property>> newConfig =
        new TreeMap<String, Map<String, Property>>();
    for(Stack stack: stacks) {
      mergeInConfiguration(newConfig, stack.getConfiguration(), false);
    }
    return buildConfig(newConfig);
  }

  private Configuration flattenConfiguration(List<Stack> stacks,
                                             String componentName,
                                             String roleName) {
    Map<String, Map<String, Property>> newConfig =
        new TreeMap<String, Map<String,Property>>();
    for(Stack stack: stacks) {
      mergeInConfiguration(newConfig, stack.getConfiguration(), true);
      for (Component component: stack.getComponents()) {
        if (component.getName().equals(componentName)) {
          mergeInConfiguration(newConfig, component.getConfiguration(), true);
          List<Role> roleList = component.getRoles();
          if (roleList != null) {
            for (Role role: roleList) {
              if (role.getName().equals(roleName)) {
                mergeInConfiguration(newConfig, role.getConfiguration(), true);
              }
            }
          }
        }
      }
    }
    return buildConfig(newConfig);
  }

  private Component flattenComponent(String name, 
                                     List<Stack> stacks) throws IOException {
    Component result = null;
    for(Stack stack: stacks) {
      for(Component comp: stack.getComponents()) {
        if (comp.getName().equals(name)) {
          if (result != null) {
            result.mergeInto(comp);
          } else {
            result = new Component();
            result.setDefinition(new ComponentDefinition());
            result.mergeInto(comp);
          }
        }
      }
    }
    // we don't want the component config
    result.setConfiguration(null);
    List<Role> roles = new ArrayList<Role>();
    result.setRoles(roles);
    ComponentPlugin plugin = plugins.getPlugin(result.getDefinition());
    for(String roleName: plugin.getActiveRoles()) {
      Role role = new Role();
      roles.add(role);
      role.setName(roleName);
      role.setConfiguration(flattenConfiguration(stacks, name, roleName));
    }
    return result;
  }

  @Inject
  StackFlattener(Stacks stacks, ComponentPluginFactory plugins) {
    this.stacks = stacks;
    this.plugins = plugins;
  }

  public Stack flattenStack(String stackName, int stackRevision
                            ) throws WebApplicationException, IOException {
    List<Stack> stacks = getStackList(stackName, stackRevision);
    Stack result = new Stack(stacks.get(stacks.size()-1));
    result.setParentName(null);
    result.setPackageRepositories(flattenRepositories(stacks));
    result.setDefault_user_group(flattenUserGroup(stacks));
    List<Component> components = new ArrayList<Component>();
    result.setComponents(components);
    for(String componentName: getComponents(stacks)) {
      components.add(flattenComponent(componentName, stacks));
    }
    result.setConfiguration(buildClientConfiguration(stacks));
    /*
     * Set the default stack level user/group info, if it is not set 
     * at the component level.
     */
    for (Component comp : components) {
        if (comp.getUser_group() == null) {
            comp.setUser_group(result.getDefault_user_group());
        }
    }
    return result;
  }
}
