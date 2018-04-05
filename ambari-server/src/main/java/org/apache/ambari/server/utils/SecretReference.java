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

package org.apache.ambari.server.utils;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.SetMultimap;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.stack.StackDirectory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.topology.Configuration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.apache.ambari.server.topology.ServiceInstance;
import org.apache.commons.lang3.StringUtils;


@StaticallyInject
public class SecretReference {
  public static final String SECRET_PREFIX = "SECRET";
  private String configType;
  private Long version;
  private String value;

  private final static String PASSWORD_TEXT = "password";
  private final static String PASSWD_TEXT = "passwd";

  @Inject
  private static Gson gson;

  public SecretReference(String reference, Cluster cluster) throws AmbariException{
    String[] values = reference.split(":");

    configType = values[1];
    version = Long.valueOf(values[2]);

    String propertyName = values[3];
    String clusterName = cluster.getClusterName();
    Config refConfig = cluster.getConfigByVersion(configType, version);

    if(refConfig == null)
      throw new AmbariException(String.format("Error when parsing secret reference. Cluster: %s does not contain ConfigType: %s ConfigVersion: %s",
          clusterName, configType, version));
    Map<String, String> refProperties = refConfig.getProperties();
    if(!refProperties.containsKey(propertyName))
      throw new AmbariException(String.format("Error when parsing secret reference. Cluster: %s ConfigType: %s ConfigVersion: %s does not contain property '%s'",
          clusterName, configType, version, propertyName));

    this.value = refProperties.get(propertyName);
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  public Long getVersion() {
    return version;
  }

  public String getValue() {
    return value;
  }

  public static boolean isSecret(String value) {
    String[] values = value.split(":");
    return values.length == 4 && values[0].equals(SECRET_PREFIX);
  }

  public static String generateStub(String configType, Long configVersion, String propertyName) {
    return SECRET_PREFIX + ":" + configType + ":" + configVersion + ":" + propertyName;
  }

  /**
   * Helper function to mask a string of property bags that may contain a property with a password.
   * @param propertyMap Property map to mask by replacing any passwords with the text "SECRET"
   * @return New string with the passwords masked, or null if the property map is null.
   */
  public static String maskPasswordInPropertyMap(String propertyMap) {
    if (null == propertyMap) return null;
    Map<String, String> maskedMap = new HashMap<>();
    Map<String, String> map = gson.fromJson(propertyMap, new TypeToken<Map<String, String>>() {}.getType());
    for (Map.Entry<String, String> e : map.entrySet()) {
      String value = e.getValue();
      if (e.getKey().toLowerCase().contains(PASSWORD_TEXT) || e.getKey().toLowerCase().contains(PASSWD_TEXT)) {
        value = SECRET_PREFIX;
      }
      maskedMap.put(e.getKey(), value);
    }
    return gson.toJson(maskedMap);
  }

  /**
   * Replace secret references with appropriate real passwords.
   * @param targetMap map in which replacement will be performed
   * @param cluster current cluster
   * @throws AmbariException
   */
  public static void replaceReferencesWithPasswords(Map<String, String> targetMap, Cluster cluster)
      throws AmbariException {
    if(cluster != null) {
      for (Map.Entry<String, String> propertyValueEntry : targetMap.entrySet()) {
        String key = propertyValueEntry.getKey();
        String value = propertyValueEntry.getValue();
        if (value != null && SecretReference.isSecret(value)) {
          SecretReference ref = new SecretReference(value, cluster);
          targetMap.put(key, ref.getValue());
        }
      }
    }
  }

  /**
   * Replace real passwords with secret references
   * @param propertiesTypes map with properties types
   * @param propertiesMap map with properties in which replacement will be performed
   * @param configType configuration type
   * @param configVersion configuration version
   */
  public static void replacePasswordsWithReferences(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes,
                                                    Map<String, String> propertiesMap,
                                                    String configType,
                                                    Long configVersion){
    if(propertiesTypes != null && propertiesTypes.containsKey(PropertyInfo.PropertyType.PASSWORD)) {
      for(String pwdPropertyName: propertiesTypes.get(PropertyInfo.PropertyType.PASSWORD)) {
        if(propertiesMap.containsKey(pwdPropertyName)){
          if(!propertiesMap.get(pwdPropertyName).equals("")) {
            String stub = SecretReference.generateStub(configType, configVersion, pwdPropertyName);
            propertiesMap.put(pwdPropertyName, stub);
          }
        }
      }
    }
  }

  /**
   * Returns all password properties defined in the stacks specified by the given stack id's. Keys in the map are
   * file names (e.g hadoop-env.xml) and values are property names.
   * @param stackIds the stack ids to specify which stacks to look for
   * @return A set multimap of password type properties.
   * @throws IllegalArgumentException when a non-existing stack is specified
   */
  public static SetMultimap<String, String> getAllPasswordProperties(Collection<StackId> stackIds) throws IllegalArgumentException {
    AmbariMetaInfo metaInfo = AmbariServer.getController().getAmbariMetaInfo();
    Collection<StackInfo> stacks = stackIds.stream().map( stackId -> {
      try {
        return metaInfo.getStack(stackId);
      }
      catch (StackAccessException ex) {
        throw new IllegalArgumentException(ex);
      }
    }).collect(toList());
    return getAllPasswordPropertiesInternal(stacks);
  }

  /**
   * Returns all password properties defined in all stacks. Keys in the map are
   * file names (e.g hadoop-env.xml) and values are property names.
   * @return A set multimap of password type properties.
   */
  public static SetMultimap<String, String> getAllPasswordProperties() {
    AmbariMetaInfo metaInfo = AmbariServer.getController().getAmbariMetaInfo();
    return getAllPasswordPropertiesInternal(metaInfo.getStacks());
  }

  private static SetMultimap<String, String> getAllPasswordPropertiesInternal(Collection<StackInfo> stacks) {
    SetMultimap<String, String> passwordPropertyMap = HashMultimap.create();
    stacks.stream().
      flatMap(stack -> stack.getServices().stream()).
      flatMap(serviceInfo -> serviceInfo.getProperties().stream()).
      filter(propertyInfo -> propertyInfo.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)).
      forEach(propertyInfo -> passwordPropertyMap.put(
        StringUtils.removeEnd(propertyInfo.getFilename(), StackDirectory.SERVICE_CONFIG_FILE_NAME_POSTFIX),
        propertyInfo.getName())
      );
    return passwordPropertyMap;
  }

  /**
   * Replace real passwords with secret references
   * @param configAttributes map with config attributes containing properties types as part of their content
   * @param propertiesMap map with properties in which replacement will be performed
   * @param configType configuration type
   * @param configVersion configuration version
   */
  public static void replacePasswordsWithReferencesForCustomProperties(Map<String, Map<String, String>> configAttributes,
                                                    Map<String, String> propertiesMap,
                                                    String configType,
                                                    Long configVersion){
    if(configAttributes != null && configAttributes.containsKey("password")) {
      for(String pwdPropertyName: configAttributes.get("password").keySet()) {
        if(propertiesMap.containsKey(pwdPropertyName)){
          if(!propertiesMap.get(pwdPropertyName).equals("")) {
            String stub = SecretReference.generateStub(configType, configVersion, pwdPropertyName);
            propertiesMap.put(pwdPropertyName, stub);
          }
        }
      }
    }
  }

  public static Configuration replacePasswordsInConfigurations(Configuration configuration,
                                                                     Multimap<String, String> passwordProperties) {
    // replace passwords in config properties
    Map<String, Map<String, String>> replacedProperties = configuration.getProperties().entrySet().stream().map(
      entry -> {
        String configType = entry.getKey();
        Map<String, String> replacedConfigProps =
          replacePasswordsInPropertyMap(entry.getValue(), configType, passwordProperties);
        return new SimpleEntry<>(configType, replacedConfigProps);
      }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    // replace passwords in config attributes
    Map<String, Map<String, Map<String, String>>> replacedAttributes = configuration.getAttributes().entrySet().stream().map(
      configTypeEntry -> {
        String configType = configTypeEntry.getKey();
        Map<String, Map<String, String>> replacedConfigProps = configTypeEntry.getValue().entrySet().stream().collect(
          toMap(Map.Entry::getKey, e -> replacePasswordsInPropertyMap(e.getValue(), configType, passwordProperties)));
        return new SimpleEntry<>(configType, replacedConfigProps);
      }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new Configuration(replacedProperties, replacedAttributes);
    }

  public static Map<String, String> replacePasswordsInPropertyMap(Map<String, String> propertyMap,
                                                                  String configType,
                                                                  Multimap<String, String> passwordProperties) {
    return propertyMap.entrySet().stream().map(entry -> {
      String propertyType = entry.getKey();
      String newValue = passwordProperties.get(configType).contains(propertyType) ?
        SECRET_PREFIX + ":" + configType + ":" + propertyType :
        entry.getValue();
      return new SimpleEntry<>(propertyType, newValue);
    }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

}
