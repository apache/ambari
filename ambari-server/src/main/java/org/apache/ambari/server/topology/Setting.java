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

package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableSet;

public class Setting {
  /**
   * Settings for this configuration instance
   */
  private final Map<String, Set<Map<String, String>>> properties;

  static final String SETTING_NAME_RECOVERY_SETTINGS = "recovery_settings";
  static final String SETTING_NAME_SERVICE_SETTINGS = "service_settings";
  static final String SETTING_NAME_COMPONENT_SETTINGS = "component_settings";
  static final String SETTING_NAME_DEPLOYMENT_SETTINGS = "deployment_settings";
  static final String SETTING_NAME_RECOVERY_ENABLED = "recovery_enabled";
  public static final String SETTING_NAME_SKIP_FAILURE = "skip_failure";
  static final String SETTING_NAME_NAME = "name";
  static final String SETTING_NAME_REPOSITORY_SETTINGS = "repository_settings";

  /**
   * When specified under the "service_settings" section, it indicates whether credential store
   * use is enabled for that service. Value is "true" or "false". Specify a value of "true"
   * only if the stack definition for the service has a credential_store_supported value of "true".
   * If credential_store_enabled is not specified, value will be taken as null and default value
   * will be picked up from the stack definition, if available.
   *   <pre>
   *     {@code
   *       {
   *         "service_settings" : [
   *           { "name" : "RANGER",
   *             "recovery_enabled" : "true",
   *             "credential_store_enabled" : "true"
   *           },
   *           :
   *       }
   *     }
   *   </pre>
   */
  private static final String SETTING_NAME_CREDENTIAL_STORE_ENABLED = "credential_store_enabled";

  /**
   * Settings.
   *
   * @param properties  setting name-->Set(property name-->property value)
   */
  public Setting(Map<String, Set<Map<String, String>>> properties) {
    this.properties = properties;
  }

  /**
   * Get the properties for this instance.
   *
   * @return map of properties for this settings instance keyed by setting name.
   */
  public Map<String, Set<Map<String, String>>> getProperties() {
    return properties;
  }

  /**
   * Get the setting properties for a specified setting name.
   *
   * @param settingName
   * @return Set of Map of properties.
   */
  public Set<Map<String, String>> getSettingValue(String settingName) {
    return properties.getOrDefault(settingName, ImmutableSet.of());
  }

  /**
   * Get whether the specified service is enabled for credential store use.
   *
   * <pre>
   *     {@code
   *       {
   *         "service_settings" : [
   *         { "name" : "RANGER",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "true"
   *         },
   *         { "name" : "HIVE",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "false"
   *         },
   *         { "name" : "TEZ",
   *           "recovery_enabled" : "false"
   *         }
   *       ]
   *     }
   *   }
   * </pre>
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getCredentialStoreEnabled(String serviceName) {
    return getStringFromNamedMap(SETTING_NAME_SERVICE_SETTINGS, serviceName, SETTING_NAME_CREDENTIAL_STORE_ENABLED);
  }

  private String getStringFromNamedMap(String outerKey, String mapName, String innerKey) {
    Set<Map<String, String>> maps = getSettingValue(outerKey);
    for (Map<String, String> map : maps) {
      String name = map.get(SETTING_NAME_NAME);
      if (Objects.equals(name, mapName)) {
        String value = map.get(innerKey);
        if (!StringUtils.isEmpty(value)) {
          return value;
        }
        break;
      }
    }
    return null;
  }

  private String getString(String outerKey, String innerKey) {
    Set<Map<String, String>> maps = getSettingValue(outerKey);
    for (Map<String, String> map : maps) {
      if (map.containsKey(innerKey)) {
        return map.get(innerKey);
      }
    }
    return null;
  }

  boolean shouldSkipFailure() {
    return Boolean.parseBoolean(getString(SETTING_NAME_DEPLOYMENT_SETTINGS, SETTING_NAME_SKIP_FAILURE));
  }

  List<RepositorySetting> processRepoSettings() {
    Set<Map<String, String>> repositorySettingsValue = getSettingValue(SETTING_NAME_REPOSITORY_SETTINGS);
    return repositorySettingsValue.stream()
      .map(RepositorySetting::fromMap)
      .collect(toList());
  }

  /**
   * Get whether the specified component in the service is enabled
   * for auto start.
   *
   * @param serviceName - Service name.
   * @param componentName - Component name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getRecoveryEnabled(String serviceName, String componentName) {
    Set<Map<String, String>> maps;

    // If component name was specified in the list of "component_settings",
    // determine if recovery_enabled is true or false and return it.
    String result = getStringFromNamedMap(SETTING_NAME_COMPONENT_SETTINGS, componentName, SETTING_NAME_RECOVERY_ENABLED);
    // If component name is not specified, look up it's service.
    if (result == null) {
      result = getStringFromNamedMap(SETTING_NAME_SERVICE_SETTINGS, serviceName, SETTING_NAME_RECOVERY_ENABLED);
    }
    // If service name is not specified, look up the cluster setting.
    if (result == null) {
      result = getString(SETTING_NAME_RECOVERY_SETTINGS, SETTING_NAME_RECOVERY_ENABLED);
    }

    return result;
  }
}
