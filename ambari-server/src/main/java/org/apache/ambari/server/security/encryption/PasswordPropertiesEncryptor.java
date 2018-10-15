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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.utils.TextEncoding;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * {@link Encryptor} implementation for encrypting/decrypting PASSWORD type properties
 *
 */
    
@Singleton
public class PasswordPropertiesEncryptor implements Encryptor<Map<String, String>> {

  private static final String ENCRYPTED_PROPERTY_PREFIX = "${enc=aes256_hex, value=";
  private static final String ENCRYPTED_PROPERTY_SCHEME = ENCRYPTED_PROPERTY_PREFIX + "%s}";

  private final EncryptionService encryptionService;
  private final Map<Long, Map<String, Set<String>>> clusterPasswordProperties = new HashMap<>();

  @Inject
  public PasswordPropertiesEncryptor(Clusters clusters, EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  @Override
  public Map<String, String> encryptSensitiveData(Map<String, String> properties, Object... additionaKeys) {
    try {
      final Map<String, String> encryptedProperties = new HashMap<>(properties.size());
      if (properties != null) {
        final Cluster cluster = (Cluster) additionaKeys[0];
        final String configType = (String) additionaKeys[1];
        final Set<String> passwordProperties = getPasswordProperties(cluster, configType);
        if (passwordProperties != null && !passwordProperties.isEmpty()) {
          String encryptedValue;
          for (Map.Entry<String, String> property : properties.entrySet()) {
            encryptedValue = passwordProperties.contains(property.getKey()) && !isEncryptedPassword(property.getValue())
                ? encryptAndDecoratePropertyValue(property.getValue())
                : property.getValue();
            encryptedProperties.put(property.getKey(), encryptedValue);
          }
        }
      }
      return encryptedProperties;
    } catch (Exception e) {
      throw new AmbariRuntimeException("Error while encrypting sensitive data", e);
    }
  }

  private boolean isEncryptedPassword(String password) {
    return password != null && password.startsWith(ENCRYPTED_PROPERTY_PREFIX); // assuming previous encryption by this class
  }

  private Set<String> getPasswordProperties(Cluster cluster, String configType) throws AmbariException {
    final long clusterId = cluster.getClusterId();
    if (!clusterPasswordProperties.containsKey(clusterId)) {
      clusterPasswordProperties.put(clusterId, new HashMap<>());
    }
    if (!clusterPasswordProperties.get(clusterId).containsKey(configType)) {
      clusterPasswordProperties.get(clusterId).put(configType, cluster.getConfigPropertiesTypes(configType).get(PropertyType.PASSWORD));
    }

    return clusterPasswordProperties.get(clusterId).get(configType);
  }

  private String encryptAndDecoratePropertyValue(String propertyValue) throws Exception {
    final String encrypted = encryptionService.encrypt(propertyValue, TextEncoding.BIN_HEX);
    return String.format(ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  @Override
  public Map<String, String> decryptSensitiveData(Map<String, String> properties, Object... additionalInfo) {
    final Map<String, String> decryptedProperties = new HashMap<>(properties.size());
    if (properties != null) {
      String decryptedValue;
      for (Map.Entry<String, String> property : properties.entrySet()) {
        decryptedValue = isEncryptedPassword(property.getValue()) ? decryptProperty(property.getValue()) : property.getValue();
        decryptedProperties.put(property.getKey(), decryptedValue);
      }
    }
    return decryptedProperties;
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
