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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.PropertyInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


/**
 * Configuration module which provides functionality related to parsing and fully
 * resolving a configuration from the stack definition. Each instance is specific
 * to a configuration type.
 */
public class ConfigurationModule extends BaseModule<ConfigurationModule, ConfigurationInfo> {
  /**
   * Configuration type
   */
  private String configType;

  /**
   * Associated configuration info
   */
  ConfigurationInfo info;

  /**
   * Specifies whether this configuration is marked as deleted
   */
  private boolean isDeleted;


  /**
   * Constructor.
   *
   * @param configType  configuration type
   * @param info        configuration info
   */
  public ConfigurationModule(String configType, ConfigurationInfo info) {
    this.configType = configType;
    this.info = info;
  }

  @Override
  public void resolve(ConfigurationModule parent, Map<String, StackModule> allStacks) throws AmbariException {
    // merge properties also removes deleted props so should be called even if extension is disabled
    mergeProperties(parent);

    if (isExtensionEnabled()) {
      mergeAttributes(parent);
    }
  }

  @Override
  public ConfigurationInfo getModuleInfo() {
    return info;
  }

  @Override
  public boolean isDeleted() {
    return isDeleted;
  }

  @Override
  public String getId() {
    return getConfigType();
  }

  /**
   * Obtain the configuration type.
   *
   * @return configuration type
   */
  public String getConfigType() {
    return configType;
  }


  /**
   * Set the deleted flag.
   *
   * @param isDeleted  whether the configuration has been marked for deletion
   */
  public void setDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  /**
   * Merge configuration properties with the configurations parent.
   *
   * @param parent  parent configuration module
   */
  private void mergeProperties(ConfigurationModule parent) {
    Collection<String> existingProps = new HashSet<String>();
    Iterator<PropertyInfo> iter = info.getProperties().iterator();
    while (iter.hasNext()) {
      PropertyInfo prop = iter.next();
      existingProps.add(prop.getFilename() + "/" + prop.getName());
      if (prop.isDeleted()) {
        iter.remove();
      }
    }

    if (isExtensionEnabled()) {
      for (PropertyInfo prop : parent.info.getProperties()) {
        if (! existingProps.contains(prop.getFilename() + "/" + prop.getName())) {
          info.getProperties().add(prop);
        }
      }
    }
  }

  /**
   * Merge configuration attributes with the parent configuration.
   *
   * @param parent  parent configuration module
   */
  private void mergeAttributes(ConfigurationModule parent) {

    for (Map.Entry<String, Map<String, String>> parentCategoryEntry : parent.info.getAttributes().entrySet()) {
      String category = parentCategoryEntry.getKey();
      Map<String, String> categoryAttributeMap = info.getAttributes().get(category);
      if (categoryAttributeMap == null) {
        categoryAttributeMap = new HashMap<String, String>();
        info.getAttributes().put(category, categoryAttributeMap);
      }
      for (Map.Entry<String, String> parentAttributeEntry : parentCategoryEntry.getValue().entrySet()) {
        String attributeName = parentAttributeEntry.getKey();
        if (! categoryAttributeMap.containsKey(attributeName)) {
          categoryAttributeMap.put(attributeName, parentAttributeEntry.getValue());
        }
      }
    }
  }

  /**
   * Determine if the configuration should extend the parents configuration.
   *
   * @return true if this configuration should extend the parents; false otherwise
   */
  //todo: is this valuable as a generic module concept?
  private boolean isExtensionEnabled() {
    Map<String, String> supportsMap = getModuleInfo().getAttributes().get(ConfigurationInfo.Supports.KEYWORD);
    if (supportsMap == null) {
      return true;
    }

    String val = supportsMap.get(ConfigurationInfo.Supports.DO_NOT_EXTEND.getPropertyName());
    return val == null || val.equals("false");
  }
}
