/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.TextEncoding;
import org.apache.commons.collections.CollectionUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * {@link Encryptor} implementation for encrypting/decrypting PASSWORD type
 * properties
 */

@Singleton
public class PasswordPropertiesEncryptor implements Encryptor<Map<String, String>> {

  private static final String ENCRYPTED_PROPERTY_PREFIX = "${enc=aes256_hex, value=";
  private static final String ENCRYPTED_PROPERTY_SCHEME = ENCRYPTED_PROPERTY_PREFIX + "%s}";

  private final EncryptionService encryptionService;
  private final Map<Long, Map<StackId, Map<String, Set<String>>>> clusterPasswordProperties = new ConcurrentHashMap<>(); //Map<clusterId, <Map<stackId, Map<configType, Set<passwordPropertyKeys>>>>;

  @Inject
  public PasswordPropertiesEncryptor(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  /**
   * It is a must that any caller of this method adds the following additional
   * information on the following indexes in <code>additionalInfo</code> with the
   * following types:
   * <ul>
   * <li>[0] - <code>org.apache.ambari.server.state.Cluster</code> (the cluster
   * whose properties should be scanned for PASSWORD properties)</li>
   * <li>[1] - <code>java.lang.String</code> (to keep properties only that match
   * this configuration type )</li>
   * </ul>
   */
  @Override
  public Map<String, String> encryptSensitiveData(Map<String, String> properties, Object... additionaInfo) {
    try {
      if (properties != null) {
        final Map<String, String> encryptedProperties = new HashMap<>(properties);
        final Cluster cluster = (Cluster) additionaInfo[0];
        final String configType = (String) additionaInfo[1];
        final Set<String> passwordProperties = getPasswordProperties(cluster, configType);
        if (CollectionUtils.isNotEmpty(passwordProperties)) {
          for (Map.Entry<String, String> property : properties.entrySet()) {
            if (passwordProperties.contains(property.getKey()) && !isEncryptedPassword(property.getValue())) {
              encryptedProperties.put(property.getKey(), encryptAndDecoratePropertyValue(property.getValue()));
            }
          }
        }
        return encryptedProperties;
      }
      return properties;
    } catch (Exception e) {
      throw new AmbariRuntimeException("Error while encrypting sensitive data", e);
    }
  }

  private boolean isEncryptedPassword(String password) {
    return password != null && password.startsWith(ENCRYPTED_PROPERTY_PREFIX); // assuming previous encryption by this class
  }

  private Set<String> getPasswordProperties(Cluster cluster, String configType) throws AmbariException {
    //in case of normal configuration change on the UI - or via the API - the current and desired stacks are equal
    //in case of an upgrade they are different; in this case we want to get password properties from the desired stack
    if (cluster.getCurrentStackVersion().equals(cluster.getDesiredStackVersion())) {
      return getPasswordProperties(cluster, cluster.getCurrentStackVersion(), configType);
    } else {
      return getPasswordProperties(cluster, cluster.getDesiredStackVersion(), configType);
    }
  }

  private Set<String> getPasswordProperties(Cluster cluster, StackId stackId, String configType) {
    final long clusterId = cluster.getClusterId();
    clusterPasswordProperties.computeIfAbsent(clusterId, v -> new ConcurrentHashMap<>()).computeIfAbsent(stackId, v -> new ConcurrentHashMap<>())
        .computeIfAbsent(configType, v -> cluster.getConfigPropertiesTypes(configType, stackId).getOrDefault(PropertyType.PASSWORD, new HashSet<>()));
    return clusterPasswordProperties.get(clusterId).get(stackId).getOrDefault(configType, new HashSet<>());
  }

  private String encryptAndDecoratePropertyValue(String propertyValue) throws Exception {
    final String encrypted = encryptionService.encrypt(propertyValue, TextEncoding.BIN_HEX);
    return String.format(ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  @Override
  public Map<String, String> decryptSensitiveData(Map<String, String> properties, Object... additionalInfo) {
    if (properties != null) {
      final Map<String, String> decryptedProperties = new HashMap<>(properties);
      for (Map.Entry<String, String> property : properties.entrySet()) {
        if (isEncryptedPassword(property.getValue())) {
          decryptedProperties.put(property.getKey(), decryptProperty(property.getValue()));
        }
      }
      return decryptedProperties;
    }
    return properties;
  }

  private String decryptProperty(String property) {
    try {
      // sample value: ${enc=aes256_hex, value=5248...303d}
      final String encrypted = property.substring(ENCRYPTED_PROPERTY_PREFIX.length(), property.indexOf('}'));
      return encryptionService.decrypt(encrypted, TextEncoding.BIN_HEX);
    } catch (Exception e) {
      throw new AmbariRuntimeException("Error while decrypting property", e);
    }
  }
}
